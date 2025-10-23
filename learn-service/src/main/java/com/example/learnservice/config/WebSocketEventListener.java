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

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<Long, Set<WebSocketController.UserStatus>> testRooms;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        log.info("WebSocket disconnect event received");

        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        Long semesterTestId = (Long) headerAccessor.getSessionAttributes().get("semesterTestId");
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");

        if (semesterTestId != null && userId != null) {
            log.info("User {} disconnecting from test {}", userId, semesterTestId);

            // Remove user from room
            Set<WebSocketController.UserStatus> users = testRooms.get(semesterTestId);
            if (users != null) {
                users.removeIf(u -> u.userId().equals(userId));

                // Remove room if empty
                if (users.isEmpty()) {
                    testRooms.remove(semesterTestId);
                    log.info("Test room {} removed (empty)", semesterTestId);
                } else {
                    // Broadcast updated list
                    broadcastUserList(semesterTestId, users);
                }

                log.info("User {} disconnected from test room {}", userId, semesterTestId);
            }
        }
    }

    private void broadcastUserList(Long semesterTestId, Set<WebSocketController.UserStatus> users) {
        // Count by status
        long waitingCount = users.stream()
                .filter(u -> u.status() == WebSocketController.TestStatus.WAITING)
                .count();
        long testingCount = users.stream()
                .filter(u -> u.status() == WebSocketController.TestStatus.TESTING)
                .count();
        long submittedCount = users.stream()
                .filter(u -> u.status() == WebSocketController.TestStatus.SUBMITTED)
                .count();

        WebSocketController.TestRoomUpdate update = new WebSocketController.TestRoomUpdate(
                semesterTestId,
                users,
                users.size(),
                (int) waitingCount,
                (int) testingCount,
                (int) submittedCount);

        messagingTemplate.convertAndSend(
                "/topic/test/" + semesterTestId + "/users",
                update);
    }
}