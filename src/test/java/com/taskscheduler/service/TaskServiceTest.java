package com.taskscheduler.service;

import com.taskscheduler.model.Task;
import com.taskscheduler.model.Task.TaskPriority;
import com.taskscheduler.model.Task.TaskStatus;
import com.taskscheduler.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    
    @Mock
    private TaskRepository taskRepository;
    
    @InjectMocks
    private TaskService taskService;
    
    private Task testTask;
    
    @BeforeEach
    void setUp() {
        testTask = Task.builder()
                .id(1L)
                .name("Test Task")
                .description("Test Description")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.HIGH)
                .scheduledTime(LocalDateTime.now().plusHours(1))
                .retryCount(0)
                .maxRetries(3)
                .build();
    }
    
    @Test
    void createTask_ShouldSetPendingStatus() {
        // Arrange
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        
        // Act
        Task result = taskService.createTask(testTask);
        
        // Assert
        assertNotNull(result);
        assertEquals(TaskStatus.PENDING, result.getStatus());
        verify(taskRepository, times(1)).save(any(Task.class));
    }
    
    @Test
    void getTask_WhenExists_ShouldReturnTask() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        
        // Act
        Task result = taskService.getTask(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Task", result.getName());
    }
    
    @Test
    void getTask_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> taskService.getTask(999L));
    }
    
    @Test
    void updateTaskStatus_ShouldUpdateAndSave() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        
        // Act
        Task result = taskService.updateTaskStatus(1L, TaskStatus.RUNNING);
        
        // Assert
        assertEquals(TaskStatus.RUNNING, result.getStatus());
        assertNotNull(result.getStartedAt());
        verify(taskRepository, times(1)).save(any(Task.class));
    }
    
    @Test
    void cancelTask_ShouldSetCancelledStatus() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        
        // Act
        taskService.cancelTask(1L);
        
        // Assert
        verify(taskRepository, times(1)).save(argThat(task -> 
            task.getStatus() == TaskStatus.CANCELLED && 
            task.getCompletedAt() != null
        ));
    }
}