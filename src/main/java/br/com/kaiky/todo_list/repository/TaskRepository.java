package br.com.kaiky.todo_list.repository;

import java.util.Optional;
import java.util.UUID;

import br.com.kaiky.todo_list.entity.Task;
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
}
