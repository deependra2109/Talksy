'use strict';


const API_BASE = 'http://localhost:8080';
const WS_URL   = 'http://localhost:8080/ws';

let token      = null;
let myUsername = null;
let stomp      = null;
let wsReady    = false;

const store   = {};  
const unseen  = {};   
const rooms   = [];   
const dms     = [];   

let activeCh = null;  
let searchTm = null;

function el(id) { return document.getElementById(id); }


function switchTab(tab) {
  el('form-login').style.display    = tab === 'login'    ? 'flex' : 'none';
  el('form-register').style.display = tab === 'register' ? 'flex' : 'none';

  el('tab-login').classList.toggle('active',    tab === 'login');
  el('tab-register').classList.toggle('active', tab === 'register');

  clearErr();
}

function showErr(msg) {
  const box = el('auth-error');
  box.textContent  = msg;
  box.style.display = 'block';
}
function clearErr() { el('auth-error').style.display = 'none'; }


async function doRegister() {
  clearErr();
  const username = el('reg-username').value.trim();
  const email    = el('reg-email').value.trim();
  const password = el('reg-password').value;

  if (!username || !email || !password) {
    showErr('All fields are required.');
    return;
  }

  try {
    const r = await post('/api/auth/register', { username, email, password });
    const d = await r.json();
    if (!r.ok) { showErr(d.message || 'Registration failed.'); return; }
    bootApp(d);
  } catch (e) {
    showErr('Cannot reach server — is the backend running on port 8080?');
  }
}


async function doLogin() {
  clearErr();
  const username = el('login-username').value.trim();
  const password = el('login-password').value;

  if (!username || !password) {
    showErr('Enter your username and password.');
    return;
  }

  try {
    const r = await post('/api/auth/login', { username, password });
    const d = await r.json();
    if (!r.ok) { showErr(d.message || 'Login failed.'); return; }
    bootApp(d);
  } catch (e) {
    showErr('Cannot reach server — is the backend running on port 8080?');
  }
}


function bootApp(data) {
  token      = data.token;
  myUsername = data.username;

  el('auth-screen').style.display = 'none';
  el('app-screen').classList.remove('app-hidden');

  el('me-name').textContent   = myUsername;
  el('me-avatar').textContent = myUsername[0].toUpperCase();

  ['general', 'random'].forEach(addRoomLocal);
  renderSidebar();
  connectWS();
}


function doLogout() {
  if (stomp) { try { stomp.disconnect(); } catch (_) {} }

  token = null; myUsername = null;
  stomp = null; wsReady = false;

  rooms.length = 0; dms.length = 0;
  activeCh = null;
  Object.keys(store).forEach(k  => delete store[k]);
  Object.keys(unseen).forEach(k => delete unseen[k]);

  el('app-screen').classList.add('app-hidden');
  el('auth-screen').style.display = 'flex';
  el('rooms-list').innerHTML   = '';
  el('dms-list').innerHTML     = '';
  el('feed-list').innerHTML    = '';
  el('channel-view').style.display = 'none';
  el('empty-state').style.display  = 'flex';
  setDot('off');
}


function connectWS() {
  setDot('ing');
  const sock = new SockJS(WS_URL);
  stomp = Stomp.over(sock);
  stomp.debug = null;

  stomp.connect(
    { Authorization: 'Bearer ' + token },
    function onOk() {
      wsReady = true;
      setDot('on');
      toast('Connected', 'ok');

      stomp.subscribe('/user/queue/messages', function(frame) {
        onPrivate(JSON.parse(frame.body));
      });

      stomp.subscribe('/user/queue/errors', function(frame) {
        const err = JSON.parse(frame.body);
        injectError(err.content || 'Server error.');
        toast(err.content || 'Error', 'bad');
      });

      rooms.forEach(subscribeRoom);

      rooms.forEach(loadGroupHistory);
      renderSidebar();
    },
    function onFail() {
      wsReady = false;
      setDot('off');
      toast('WebSocket disconnected', 'bad');
    }
  );
}

function subscribeRoom(room) {
  if (!stomp || !wsReady) return;
  stomp.subscribe('/topic/chat/' + room, function(frame) {
    onRoomMsg(room, JSON.parse(frame.body));
  });
}

function joinRoom(room) {
  if (!stomp || !wsReady) return;
  stomp.send('/app/chat.join', {}, JSON.stringify({ room: room }));
}


function sendMessage() {
  const ta      = el('compose-input');
  const content = ta.value.trim();
  if (!content || !wsReady || !activeCh) return;

  if (activeCh.type === 'room') {
    stomp.send('/app/chat.group', {}, JSON.stringify({
      room: activeCh.name, content: content
    }));
  } else {
    stomp.send('/app/chat.private', {}, JSON.stringify({
      recipientUsername: activeCh.name, content: content
    }));
  }

  ta.value = '';
  ta.style.height = 'auto';
  ta.focus();
}

function handleKey(e) {
  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(); }
}
function growInput(el) {
  el.style.height = 'auto';
  el.style.height = Math.min(el.scrollHeight, 120) + 'px';
}


function onRoomMsg(room, msg) {
  if (!store[room]) store[room] = [];
  store[room].push(msg);

  const open = activeCh && activeCh.type === 'room' && activeCh.name === room;
  if (open) {
    appendMsg(msg);
    scrollBottom();
  } else if (msg.type === 'GROUP') {
    unseen[room] = (unseen[room] || 0) + 1;
    renderSidebar();
  }
}

function onPrivate(msg) {
  const peer = msg.senderUsername === myUsername
    ? msg.recipientUsername
    : msg.senderUsername;

  if (!store[peer])  store[peer] = [];
  store[peer].push(msg);
  if (!dms.includes(peer)) dms.push(peer);

  const open = activeCh && activeCh.type === 'dm' && activeCh.name === peer;
  if (open) {
    appendMsg(msg);
    scrollBottom();
  } else {
    unseen[peer] = (unseen[peer] || 0) + 1;
  }
  renderSidebar();
}


async function loadGroupHistory(room) {
  try {
    const r = await get('/api/chat/history/group/' + room + '?limit=50');
    if (!r.ok) return;
    store[room] = await r.json();
    if (activeCh && activeCh.type === 'room' && activeCh.name === room) renderAllMsgs();
  } catch (_) {}
}

async function loadDmHistory(peer) {
  try {
    const r = await get('/api/chat/history/private/' + peer + '?limit=50');
    if (!r.ok) return;
    store[peer] = await r.json();
    if (activeCh && activeCh.type === 'dm' && activeCh.name === peer) renderAllMsgs();
  } catch (_) {}
}


function selectRoom(room) {
  activeCh        = { type: 'room', name: room };
  unseen[room]    = 0;
  if (!store[room]) store[room] = [];

  openChannelView('#', room, 'group room');
  el('compose-input').placeholder = 'Message #' + room;
  renderSidebar();
  renderAllMsgs();
  joinRoom(room);
  scrollBottom();
}

function selectDm(peer) {
  activeCh       = { type: 'dm', name: peer };
  unseen[peer]   = 0;
  if (!store[peer]) store[peer] = [];
  if (!dms.includes(peer)) dms.push(peer);

  openChannelView('●', peer, 'direct message');
  el('compose-input').placeholder = 'Message ' + peer;
  renderSidebar();
  loadDmHistory(peer);
  renderAllMsgs();
  scrollBottom();
}

function openChannelView(icon, name, sub) {
  el('empty-state').style.display   = 'none';
  el('channel-view').style.display  = 'flex';
  el('ch-icon').textContent = icon;
  el('ch-name').textContent = name;
  el('ch-sub').textContent  = sub;
}


function renderAllMsgs() {
  if (!activeCh) return;
  const list  = el('feed-list');
  const msgs  = store[activeCh.name] || [];

  el('feed-empty').style.display = msgs.length ? 'none' : 'flex';
  list.innerHTML = '';

  msgs.forEach(function(msg) {
    const row = buildRow(msg);
    if (row) list.appendChild(row);
  });
  scrollBottom();
}

function appendMsg(msg) {
  el('feed-empty').style.display = 'none';
  const row = buildRow(msg);
  if (row) el('feed-list').appendChild(row);
  scrollBottom();
}

function injectError(text) {
  if (!activeCh) return;
  const row = document.createElement('div');
  row.className = 'msg-row err';
  row.innerHTML = '<div class="bubble">&#9888; ' + esc(text) + '</div>';
  el('feed-list').appendChild(row);
  scrollBottom();
  setTimeout(function() { try { row.remove(); } catch(_){} }, 6000);
}


function buildRow(msg) {
  const d = document.createElement('div');

  if (msg.type === 'JOIN' || msg.type === 'LEAVE') {
    d.className = 'msg-row system';
    d.innerHTML = '<div class="bubble">' + esc(msg.content) + '</div>';
    return d;
  }
  if (msg.type === 'ERROR') {
    d.className = 'msg-row err';
    d.innerHTML = '<div class="bubble">&#9888; ' + esc(msg.content) + '</div>';
    return d;
  }

  const mine   = msg.senderUsername === myUsername;
  const inRoom = activeCh && activeCh.type === 'room';
  const when   = msg.sentAt ? fmtTime(msg.sentAt) : '';

  d.className = 'msg-row ' + (mine ? 'mine' : 'theirs');

  const senderLine = (!mine && inRoom)
    ? '<div class="msg-meta"><span class="msg-who">' + esc(msg.senderUsername) + '</span>'
      + '<span class="msg-when">' + when + '</span></div>'
    : '';

  const tsLine = (mine || !inRoom)
    ? '<div class="bubble-ts">' + when + '</div>'
    : '';

  d.innerHTML = senderLine
    + '<div class="bubble">' + esc(msg.content) + '</div>'
    + tsLine;

  return d;
}


function renderSidebar() {
  el('rooms-list').innerHTML = rooms.map(function(room) {
    const active = activeCh && activeCh.type === 'room' && activeCh.name === room;
    const b = unseen[room] > 0
      ? '<span class="badge">' + unseen[room] + '</span>' : '';
    return '<div class="nav-item' + (active ? ' active' : '') + '" onclick="selectRoom(\'' + room + '\')">'
      + '<span class="nav-hash">#</span>'
      + '<span class="nav-label">' + room + '</span>' + b + '</div>';
  }).join('');

  el('dms-list').innerHTML = dms.map(function(peer) {
    const active = activeCh && activeCh.type === 'dm' && activeCh.name === peer;
    const b = unseen[peer] > 0
      ? '<span class="badge">' + unseen[peer] + '</span>' : '';
    const init = peer[0].toUpperCase();
    return '<div class="nav-item' + (active ? ' active' : '') + '" onclick="selectDm(\'' + peer + '\')">'
      + '<div class="avatar" style="width:20px;height:20px;font-size:9px">' + init + '</div>'
      + '<span class="nav-label">' + peer + '</span>' + b + '</div>';
  }).join('');
}


function toggleAddRoom() {
  el('add-room-row').classList.toggle('open');
  if (el('add-room-row').classList.contains('open')) el('new-room-input').focus();
}

function addRoom() {
  const input = el('new-room-input');
  const room  = input.value.trim().toLowerCase().replace(/[^a-z0-9-]/g, '');
  if (!room) return;
  input.value = '';
  el('add-room-row').classList.remove('open');
  addRoomLocal(room);
  if (wsReady) subscribeRoom(room);
  renderSidebar();
  selectRoom(room);
}

function addRoomLocal(room) {
  if (!rooms.includes(room)) {
    rooms.push(room);
    store[room]  = store[room]  || [];
    unseen[room] = unseen[room] || 0;
  }
}


function toggleUserSearch() {
  const w = el('user-search-wrap');
  w.style.display = w.style.display === 'none' ? 'block' : 'none';
  if (w.style.display === 'block') el('user-search-input').focus();
}

function debounceSearch(q) {
  clearTimeout(searchTm);
  const res = el('user-search-results');
  if (!q || q.length < 2) { res.innerHTML = ''; return; }
  searchTm = setTimeout(function() { runSearch(q); }, 300);
}

async function runSearch(q) {
  const res = el('user-search-results');
  try {
    const r     = await get('/api/users/search?q=' + encodeURIComponent(q));
    const users = await r.json();
    const list  = users.filter(function(u) { return u.username !== myUsername; });
    if (!list.length) {
      res.innerHTML = '<div class="search-none">No users found</div>';
      return;
    }
    res.innerHTML = list.map(function(u) {
      return '<div class="search-item" onclick="pickDm(\'' + u.username + '\')">'
        + '<div class="avatar" style="width:20px;height:20px;font-size:9px">' + u.username[0].toUpperCase() + '</div>'
        + '<span>' + esc(u.username) + '</span></div>';
    }).join('');
  } catch (_) {
    res.innerHTML = '<div class="search-none">Search failed</div>';
  }
}

function pickDm(peer) {
  el('user-search-wrap').style.display = 'none';
  el('user-search-input').value = '';
  el('user-search-results').innerHTML = '';
  selectDm(peer);
}


function setDot(state) {
  const dot = el('conn-dot');
  const txt = el('conn-txt');
  dot.className = 'conn-dot';
  if (state === 'on')  { dot.classList.add('on');  txt.textContent = 'Connected'; }
  else if (state === 'ing') { dot.classList.add('ing'); txt.textContent = 'Connecting…'; }
  else                 { txt.textContent = 'Disconnected'; }
}


function toast(msg, type) {
  const t = document.createElement('div');
  t.className = 'toast ' + (type || '');
  t.textContent = msg;
  el('toasts').appendChild(t);
  setTimeout(function() { try { t.remove(); } catch(_){} }, 3500);
}


function scrollBottom() {
  const feed = el('msg-feed');
  requestAnimationFrame(function() { feed.scrollTop = feed.scrollHeight; });
}

function fmtTime(iso) {
  const d = new Date(iso);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function esc(str) {
  if (!str) return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}


function get(path) {
  return fetch(API_BASE + path, {
    headers: { 'Authorization': 'Bearer ' + token }
  });
}

function post(path, body) {
  return fetch(API_BASE + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
}