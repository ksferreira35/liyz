package br.com.kaiky.todo_list.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
public class Task {

    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    private UUID id;
    
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    private boolean completed;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "due_at")
    private LocalDateTime dueAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false)
    private NotificationStatus notificationStatus = NotificationStatus.NONE;
    @Column(name = "notification_attempts", nullable = false)
    private int notificationAttempts;
    @Column(name = "notification_queued_at")
    private LocalDateTime notificationQueuedAt;
    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;
    @Column(name = "last_notification_error", length = 1000)
    private String lastNotificationError;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    public Task() {
    }

    public Task(boolean completed) {
        this.completed = completed;
    }

    public Task(String title, String description) {
        this.title = title;
        this.description = description;
        this.completed = false;
    }
    
    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public boolean isCompleted() {
        return completed;
    }
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public LocalDateTime getDueAt() {
        return dueAt;
    }
    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }
    public NotificationStatus getNotificationStatus() {
        return notificationStatus;
    }
    public void setNotificationStatus(NotificationStatus notificationStatus) {
        this.notificationStatus = notificationStatus;
    }
    public int getNotificationAttempts() {
        return notificationAttempts;
    }
    public void setNotificationAttempts(int notificationAttempts) {
        this.notificationAttempts = notificationAttempts;
    }
    public LocalDateTime getNotificationQueuedAt() {
        return notificationQueuedAt;
    }
    public void setNotificationQueuedAt(LocalDateTime notificationQueuedAt) {
        this.notificationQueuedAt = notificationQueuedAt;
    }
    public LocalDateTime getNotifiedAt() {
        return notifiedAt;
    }
    public void setNotifiedAt(LocalDateTime notifiedAt) {
        this.notifiedAt = notifiedAt;
    }
    public String getLastNotificationError() {
        return lastNotificationError;
    }
    public void setLastNotificationError(String lastNotificationError) {
        this.lastNotificationError = lastNotificationError;
    }
    public AppUser getUser() {
        return user;
    }
    public void setUser(AppUser user) {
        this.user = user;
    }
}
