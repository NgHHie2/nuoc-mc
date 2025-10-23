// learn-service/src/main/java/com/example/learnservice/controller/WebSocketController.java
package com.example.learnservice.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    // In-memory storage - KHÔNG CẦN DB
    // Key: semesterTestId, Value: Set of UserInfo
    private final Map<Long, Set<UserInfo>> waitingRooms;

    /**
     * User join waiting room
     */
    @MessageMapping("/test/{semesterTestId}/join")
    public void joinWaitingRoom(
            @DestinationVariable Long semesterTestId,
            @Payload UserJoinMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        UserInfo userInfo = new UserInfo(
                message.userId(),
                message.fullName(),
                message.cccd());

        // Store session ID để xử lý disconnect
        headerAccessor.getSessionAttributes().put("semesterTestId", semesterTestId);
        headerAccessor.getSessionAttributes().put("userId", message.userId());

        // Add user to room
        waitingRooms.computeIfAbsent(semesterTestId, k -> new CopyOnWriteArraySet<>())
                .add(userInfo);

        log.info("User {} joined waiting room {}", message.userId(), semesterTestId);

        // Broadcast updated list to all users in this room
        broadcastUserList(semesterTestId);
    }

    /**
     * User leave waiting room
     */
    @MessageMapping("/test/{semesterTestId}/leave")
    public void leaveWaitingRoom(
            @DestinationVariable Long semesterTestId,
            @Payload UserLeaveMessage message) {

        Set<UserInfo> users = waitingRooms.get(semesterTestId);
        if (users != null) {
            users.removeIf(u -> u.userId().equals(message.userId()));

            // Remove room if empty
            if (users.isEmpty()) {
                waitingRooms.remove(semesterTestId);
            }
        }

        log.info("User {} left waiting room {}", message.userId(), semesterTestId);

        // Broadcast updated list
        broadcastUserList(semesterTestId);
    }

    /**
     * Broadcast current user list to all users in room
     */
    private void broadcastUserList(Long semesterTestId) {
        Set<UserInfo> users = waitingRooms.get(semesterTestId);

        WaitingRoomUpdate update = new WaitingRoomUpdate(
                semesterTestId,
                users != null ? users : Set.of(),
                users != null ? users.size() : 0);

        messagingTemplate.convertAndSend(
                "/topic/test/" + semesterTestId + "/users",
                update);
    }

    // DTOs
    public record UserJoinMessage(Long userId, String fullName, String cccd) {
    }

    public record UserLeaveMessage(Long userId) {
    }

    public record UserInfo(Long userId, String fullName, String cccd) {
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof UserInfo))
                return false;
            return userId.equals(((UserInfo) o).userId);
        }

        @Override
        public int hashCode() {
            return userId.hashCode();
        }
    }

    public record WaitingRoomUpdate(
            Long semesterTestId,
            Set<UserInfo> users,
            int totalUsers) {
    }
}