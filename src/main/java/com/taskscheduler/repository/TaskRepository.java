package com.taskscheduler.repository;

import com.taskscheduler.model.Task;
import com.taskscheduler.model.Task.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    List<Task> findByStatus(TaskStatus status);
    
    List<Task> findByStatusIn(List<TaskStatus> statuses);
    
    @Query("SELECT t FROM Task t WHERE t.status = :status AND t.scheduledTime <= :now ORDER BY t.priority DESC, t.scheduledTime ASC")
    List<Task> findTasksReadyToExecute(TaskStatus status, LocalDateTime now);
    
    @Query("SELECT t FROM Task t WHERE t.status = 'FAILED' AND t.retryCount < t.maxRetries")
    List<Task> findTasksEligibleForRetry();
    
    List<Task> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}