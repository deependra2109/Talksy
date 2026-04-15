package com.deependra.talksy.repository;

import com.deependra.talksy.entity.Message;
import com.deependra.talksy.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {


    List<Message> findByRoom(String room, Pageable pageable);


    @Query("""  
        SELECT m FROM Message m
        WHERE (m.sender = :userA AND m.recipient = :userB)
           OR (m.sender = :userB AND m.recipient = :userA)
        """)
    List<Message> findConversation(
        @Param("userA") User userA,
        @Param("userB") User userB,
        Pageable pageable
    );
}
