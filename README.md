<h1 align="center">💬 Talksy — Real-Time Chat Application</h1>


A full-stack, real-time chat application built using Spring Boot + WebSocket + JWT, supporting group messaging, private conversations, and persistent chat history.

Designed with a clean architecture and secure communication flow, Talksy demonstrates real-world backend engineering concepts like token-based authentication, event-driven messaging, and scalable API design.

---

 Key Highlights

- 🔐 **Secure Authentication** — JWT-based login system with encrypted passwords (BCrypt)
- ⚡ **Real-Time Messaging** — Instant communication using WebSocket (STOMP protocol)
- 👥 **Group Chat System** — Join rooms and broadcast messages to multiple users
- 🧍 **Private Messaging** — 1-on-1 chat using user-specific message queues
- 🗄 **Persistent Storage** — Chat history stored and retrieved from MySQL
- 📡 **Event-Driven Architecture** — JOIN / LEAVE system events handled in real-time
- ⚠️ **Targeted Error Handling** — Errors routed only to the specific user
- 🌙 **Modern UI** — Clean dark-themed interface using Vanilla JS (no frameworks)

---

## 🛠 Tech Stack

| Layer        | Technology |
|-------------|----------|
| Backend      | Java 17, Spring Boot 3 |
| Real-Time    | WebSocket, STOMP, SockJS |
| Security     | Spring Security, JWT, BCrypt |
| Database     | MySQL, Hibernate, JPA |
| Frontend     | HTML, CSS, Vanilla JavaScript |
| Build Tool   | Maven |

---

## 📁 Project Structure

```bash

 Talksy
│
├──  Talksy-Frontend
│   ├──  index.html       
│   ├──  style.css        
│   └──  script.js        
│
└──  Talksy-Backend
    ├──  pom.xml
    ├──  .gitignore
    └──  src
        ├──  main
        │   ├──  java
        │   │   └──  com/deependra/talksy
        │   │       ├──  TalksyApplication.java
        │   │       │
        │   │       ├──  config
        │   │       │   ├──  WebSocketConfig.java
        │   │       │   ├──  SecurityConfig.java
        │   │       │   └──  JwtChannelInterceptor.java
        │   │       │
        │   │       ├──  controller
        │   │       │   ├──  AuthController.java
        │   │       │   ├──  ChatController.java
        │   │       │   └──  ChatHistoryController.java
        │   │       │
        │   │       ├──  service
        │   │       │   ├──  AuthService.java
        │   │       │   ├──  MessageService.java
        │   │       │   └──  CustomUserDetailsService.java
        │   │       │
        │   │       ├──  security
        │   │       │   ├──  JwtUtil.java
        │   │       │   └──  JwtFilter.java
        │   │       │
        │   │       ├──  entity
        │   │       │   ├──  User.java
        │   │       │   └──  Message.java
        │   │       │
        │   │       ├──  repository
        │   │       │   ├──  UserRepository.java
        │   │       │   └──  MessageRepository.java
        │   │       │
        │   │       ├──  dto
        │   │       │   └──  Request/Response classes
        │   │       │
        │   │       └──  exception
        │   │           ├──  GlobalExceptionHandler.java
        │   │           └──  WebSocketExceptionHandler.java
        │   │
        │   └──  resources
        │       └──  application.properties
        │
        └──  test

```


## 🧠 System Architecture

Client (Frontend)  
↓  
REST APIs (Authentication, Data Fetching)  
↓  
Spring Boot Backend  
↓  
WebSocket Layer (Real-Time Messaging)  
↓  
Service Layer (Business Logic)  
↓  
Repository Layer (JPA/Hibernate)  
↓  
MySQL Database  





---

## 🔌 API Overview

### 🔐 Authentication APIs

| Method | Endpoint | Description |
|-------|---------|------------|
| POST  | `/api/auth/register` | Register user |
| POST  | `/api/auth/login`    | Login and receive JWT |

---

### 💬 Chat APIs

| Method | Endpoint | Description |
|-------|---------|------------|
| GET | `/api/users/search?q=` | Search users |
| GET | `/api/chat/history/group/{room}` | Group chat history |
| GET | `/api/chat/history/private/{username}` | Private chat history |

---

### ⚡ WebSocket Endpoints

| Action | Endpoint |
|-------|--------|
| Join Room | `/app/chat.join` |
| Send Group Message | `/app/chat.group` |
| Send Private Message | `/app/chat.private` |
| Receive Group Messages | `/topic/chat/{room}` |
| Receive Private Messages | `/user/queue/messages` |
| Receive Errors | `/user/queue/errors` |

---

## 🔐 Security Design

- JWT token required for all API & WebSocket communication
- User identity extracted from **server-side Principal**
- Passwords hashed using **BCrypt**
- Client cannot spoof sender identity (fully secure flow)

---

## 🗃 Database Schema

### Users Table
- id, username, email, password, created_at

### Messages Table
- id, sender_id, recipient_id, room, content, type, sent_at

Optimized with indexing for fast message retrieval.

---


## 🚀 Run Locally

Requirements  
Java 17+  
MySQL 8+  

Steps  

git clone https://github.com/deependra2109/talksy.git  
cd talksy  

cd Talksy Backend  
./mvnw spring-boot:run  

Open Talksy Frontend/index.html in browser  

Server runs at  
http://localhost:8080  

---



## 📸 Screenshots

<img width="1902" height="1015" alt="Screenshot 2026-04-16 020048" src="https://github.com/user-attachments/assets/831d28b2-2630-4c13-9a3c-0ad60d4d9a18" />

<img width="1899" height="979" alt="Screenshot 2026-04-16 020247" src="https://github.com/user-attachments/assets/b88d7a2d-836d-42eb-9825-9abfb487391d" />


---

## 🚧 Future Improvements

- Typing indicators  
- Message reactions  
- File sharing  
- Online user status  

---

## 🎯 Why This Project

This project demonstrates:

- Real-time communication using WebSocket  
- Secure authentication using JWT  
- Clean backend architecture  
- Database design and optimization  
- Full-stack development skills  

---
## 📄 License

**All Rights Reserved.**  
This project is protected. No part of this codebase may be copied, reused, distributed, or modified without explicit written permission from the author.


---

Author

Deependra Kumar
💻 Passionate about backend engineering and building scalable systems

[![LinkedIn](https://img.shields.io/badge/LinkedIn-blue?logo=linkedin&logoColor=white&style=for-the-badge)](https://www.linkedin.com/in/deependra-kumar21/)
[![GitHub](https://img.shields.io/badge/GitHub-black?logo=github&logoColor=white&style=for-the-badge)](https://github.com/deependra2109)
