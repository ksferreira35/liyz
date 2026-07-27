package br.com.kaiky.todo_list.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.kaiky.todo_list.entity.Task;
import br.com.kaiky.todo_list.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    @Query("SELECT task FROM Task task WHERE task.user.id = :userId")
    Page<Task> findAllByUserId(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Query("""
            SELECT task
            FROM Task task
            WHERE task.id = :taskId
              AND task.user.id = :userId
            """)
    Optional<Task> findByIdAndUserId(
            @Param("taskId") UUID taskId,
            @Param("userId") UUID userId
    );

    Optional<Task> findByIdAndUser_Id(UUID taskId, UUID userId);

    List<Task> findByUser_IdAndTitleIgnoreCase(UUID userId, String title);

    List<Task> findByUser_IdAndTitleContainingIgnoreCase(UUID userId, String title);

    List<Task> findByUser_IdAndNotificationStatusInOrderByDueAtAsc(
            UUID userId,
            List<NotificationStatus> notificationStatuses
    );

    List<Task> findByUser_IdAndDueAtBetweenOrderByDueAtAsc(
            UUID userId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Task> findByUser_IdAndDueAtGreaterThanEqualOrderByDueAtAsc(
            UUID userId,
            LocalDateTime start
    );

    List<Task> findByUser_IdAndDueAtIsNullOrderByCreatedAtDesc(UUID userId);

    @Query("""
            SELECT task
            FROM Task task
            WHERE task.completed = false
              AND task.dueAt <= :now
              AND (
                    task.notificationStatus = br.com.kaiky.todo_list.entity.NotificationStatus.PENDING
                    OR (
                        task.notificationStatus = br.com.kaiky.todo_list.entity.NotificationStatus.FAILED
                        AND task.notificationAttempts < :maxAttempts
                    )
              )
            ORDER BY task.dueAt ASC
            """)
    List<Task> findDueNotifications(
            @Param("now") java.time.LocalDateTime now,
            @Param("maxAttempts") int maxAttempts,
            Pageable pageable
    );
}
