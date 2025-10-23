// learn-service/src/main/java/com/example/learnservice/config/WebSocketEventListener.java
package com.example.learnservice.config;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.example.learnservice.controller.WebSocketController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<Long, Set<WebSocketController.UserInfo>> waitingRooms;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        System.out.println("hiep dep trai");
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        Long semesterTestId = (Long) headerAccessor.getSessionAttributes().get("semesterTestId");
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        System.out.println(semesterTestId + " " + userId);
        if (semesterTestId != null && userId != null) {
            // Remove user from room
            Set<WebSocketController.UserInfo> users = waitingRooms.get(semesterTestId);
            if (users != null) {
                users.removeIf(u -> u.userId().equals(userId));

                if (users.isEmpty()) {
                    waitingRooms.remove(semesterTestId);
                }

                // Broadcast updated list
                WebSocketController.WaitingRoomUpdate update = new WebSocketController.WaitingRoomUpdate(
                        semesterTestId,
                        users,
                        users.size());

                messagingTemplate.convertAndSend(
                        "/topic/test/" + semesterTestId + "/users",
                        update);

                log.info("User {} disconnected from waiting room {}", userId, semesterTestId);
            }
        }
    }
}