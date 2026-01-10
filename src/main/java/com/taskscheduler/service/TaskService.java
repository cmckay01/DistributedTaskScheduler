package com.taskscheduler.service;

import com.taskscheduler.model.Task;
import com.taskscheduler.model.Task.TaskStatus;
import com.taskscheduler.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ConcurrentHashMap<Long, Future<?>> runningTasks = new ConcurrentHashMap<>();
    
    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public Task createTask(Task task) {
        task.setStatus(TaskStatus.PENDING);
        Task savedTask = taskRepository.save(task);
        log.info("Created task with ID: {}", savedTask.getId());
        return savedTask;
    }
    
    @Cacheable(value = "tasks", key = "#id")
    public Task getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with ID: " + id));
    }
    
    @Cacheable(value = "tasks")
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }
    
    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public Task updateTaskStatus(Long id, TaskStatus status) {
        Task task = getTask(id);
        task.setStatus(status);
        
        if (status == TaskStatus.RUNNING) {
            task.setStartedAt(LocalDateTime.now());
        } else if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) {
            task.setCompletedAt(LocalDateTime.now());
        }
        
        return taskRepository.save(task);
    }
    
    @Async
    @Transactional
    public void executeTask(Task task) {
        log.info("Starting execution of task: {}", task.getId());
        
        try {
            task.setStatus(TaskStatus.RUNNING);
            task.setStartedAt(LocalDateTime.now());
            taskRepository.save(task);
            
            // Simulate task execution
            Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 5000));
            
            // Simulate 10% failure rate for testing retry logic
            if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                throw new RuntimeException("Simulated task failure");
            }
            
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            task.setResult("Task completed successfully");
            taskRepository.save(task);
            
            log.info("Task {} completed successfully", task.getId());
            
        } catch (Exception e) {
            log.error("Task {} failed: {}", task.getId(), e.getMessage());
            handleTaskFailure(task, e);
        }
    }
    
    @Transactional
    protected void handleTaskFailure(Task task, Exception e) {
        task.setRetryCount(task.getRetryCount() + 1);
        
        if (task.getRetryCount() < task.getMaxRetries()) {
            task.setStatus(TaskStatus.RETRYING);
            task.setErrorMessage(e.getMessage());
            log.info("Task {} will be retried. Attempt {}/{}", 
                    task.getId(), task.getRetryCount(), task.getMaxRetries());
        } else {
            task.setStatus(TaskStatus.FAILED);
            task.setCompletedAt(LocalDateTime.now());
            task.setErrorMessage("Max retries exceeded: " + e.getMessage());
            log.error("Task {} failed after {} retries", task.getId(), task.getRetryCount());
        }
        
        taskRepository.save(task);
    }
    
    @Scheduled(fixedRate = 5000) // Run every 5 seconds
    @Transactional
    public void scheduleTasks() {
        List<Task> pendingTasks = taskRepository.findTasksReadyToExecute(
                TaskStatus.PENDING, LocalDateTime.now());
        
        for (Task task : pendingTasks) {
            if (runningTasks.size() < 10) { // Limit concurrent tasks
                task.setStatus(TaskStatus.QUEUED);
                taskRepository.save(task);
                
                Future<?> future = executorService.submit(() -> executeTask(task));
                runningTasks.put(task.getId(), future);
            }
        }
        
        // Retry failed tasks
        List<Task> retryTasks = taskRepository.findTasksEligibleForRetry();
        for (Task task : retryTasks) {
            if (runningTasks.size() < 10) {
                task.setStatus(TaskStatus.QUEUED);
                taskRepository.save(task);
                
                Future<?> future = executorService.submit(() -> executeTask(task));
                runningTasks.put(task.getId(), future);
            }
        }
        
        // Clean up completed futures
        runningTasks.entrySet().removeIf(entry -> entry.getValue().isDone());
    }
    
    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public void cancelTask(Long id) {
        Task task = getTask(id);
        
        Future<?> future = runningTasks.get(id);
        if (future != null && !future.isDone()) {
            future.cancel(true);
            runningTasks.remove(id);
        }
        
        task.setStatus(TaskStatus.CANCELLED);
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.save(task);
        
        log.info("Task {} cancelled", id);
    }
}