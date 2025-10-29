// learn-service/src/main/java/com/example/learnservice/controller/WebSocketController.java
package com.example.learnservice.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.example.learnservice.repository.ResultRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final ResultRepository resultRepository;

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<Long, Set<UserStatus>> testRooms;

    /**
     * User join test room
     */
    @MessageMapping("/test/{semesterTestId}/join")
    public void joinTestRoom(
            @DestinationVariable Long semesterTestId,
            @Payload UserJoinMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        UserStatus userStatus = new UserStatus(
                message.userId(),
                message.fullName(),
                message.cccd(),
                TestStatus.WAITING);

        // Store session ID
        headerAccessor.getSessionAttributes().put("semesterTestId", semesterTestId);
        headerAccessor.getSessionAttributes().put("userId", message.userId());

        // Add user to room or update if exists
        Set<UserStatus> users = testRooms.get(semesterTestId);
        if (users == null) {
            users = java.util.concurrent.ConcurrentHashMap.newKeySet();
            testRooms.put(semesterTestId, users);
        }

        // Remove old status if exists
        users.removeIf(u -> u.userId().equals(message.userId()));
        // Add new status
        users.add(userStatus);

        log.info("User {} joined test room {}", message.userId(), semesterTestId);

        // Broadcast updated list
        broadcastUserList(semesterTestId);
    }

    /**
     * User leave test room
     */
    @MessageMapping("/test/{semesterTestId}/leave")
    public void leaveTestRoom(
            @DestinationVariable Long semesterTestId,
            @Payload UserLeaveMessage message) {

        Set<UserStatus> users = testRooms.get(semesterTestId);
        if (users != null) {
            users.removeIf(u -> u.userId().equals(message.userId()));

            if (users.isEmpty()) {
                testRooms.remove(semesterTestId);
            }
        }

        log.info("User {} left test room {}", message.userId(), semesterTestId);
        broadcastUserList(semesterTestId);
    }

    /**
     * Update user status (called from service when starting/ending test)
     */
    public void updateUserStatus(Long semesterTestId, Long userId, TestStatus status) {
        Set<UserStatus> users = testRooms.get(semesterTestId);
        if (users != null) {
            UserStatus oldStatus = users.stream()
                    .filter(u -> u.userId().equals(userId))
                    .findFirst()
                    .orElse(null);

            if (oldStatus != null) {
                users.remove(oldStatus);
                users.add(new UserStatus(
                        oldStatus.userId(),
                        oldStatus.fullName(),
                        oldStatus.cccd(),
                        status));

                log.info("User {} status updated to {} in test {}", userId, status, semesterTestId);
                broadcastUserList(semesterTestId);
            }
        }
    }

    /**
     * Notify all users when test is opened
     */
    public void notifyTestOpened(Long semesterTestId) {
        TestOpenedEvent event = new TestOpenedEvent(semesterTestId, true);
        messagingTemplate.convertAndSend(
                "/topic/test/" + semesterTestId + "/opened",
                event);
        log.info("Test {} opened notification sent", semesterTestId);
    }

    @MessageMapping("/test/{semesterTestId}/request-state")
    public void requestRoomState(
            @DestinationVariable Long semesterTestId,
            @Payload UserJoinMessage message) {
        log.info("User {} requested room state for test {}", message.userId(), semesterTestId);
        broadcastUserList(semesterTestId);
    }

    /**
     * Broadcast current user list to all users in room
     */
    private void broadcastUserList(Long semesterTestId) {
        Set<UserStatus> users = testRooms.get(semesterTestId);

        if (users == null || users.isEmpty()) {
            users = Collections.emptySet();
        }

        // Count by status
        long waitingCount = users.stream()
                .filter(u -> u.status() == TestStatus.WAITING)
                .count();
        long testingCount = users.stream()
                .filter(u -> u.status() == TestStatus.TESTING)
                .count();

        TestRoomUpdate update = new TestRoomUpdate(
                semesterTestId,
                users,
                users.size(),
                (int) waitingCount,
                (int) testingCount);

        messagingTemplate.convertAndSend(
                "/topic/test/" + semesterTestId + "/users",
                update);
    }

    public void notifyTestSubmitted(Long semesterTestId, Long resultId, Long userId, Float score) {
        TestSubmittedEvent event = new TestSubmittedEvent(semesterTestId, resultId, userId, score);
        messagingTemplate.convertAndSend(
                "/topic/test/" + semesterTestId + "/submitted",
                event);
        log.info("Test submitted notification sent for user {} in test {}", userId, semesterTestId);
    }

    // DTOs
    public record UserJoinMessage(Long userId, String fullName, String cccd) {
    }

    public record UserLeaveMessage(Long userId) {
    }

    public record UserStatus(Long userId, String fullName, String cccd, TestStatus status) {
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof UserStatus))
                return false;
            return userId.equals(((UserStatus) o).userId);
        }

        @Override
        public int hashCode() {
            return userId.hashCode();
        }
    }

    public record TestRoomUpdate(
            Long semesterTestId,
            Set<UserStatus> users,
            int totalUsers,
            int waitingCount,
            int testingCount) {
    }

    public record TestSubmittedEvent(Long semesterTestId, Long resultId, Long userId, Float score) {
    }

    public record TestOpenedEvent(Long semesterTestId, boolean opened) {
    }

    public enum TestStatus {
        WAITING, // Đang chờ
        TESTING // Đang thi
    }
}