package com.example.caching.controller;

import com.example.caching.exception.CacheException;
import com.example.caching.exception.EntityNotFoundException;
import com.example.caching.exception.GlobalExceptionHandler;
import com.example.caching.model.CustomEntity;
import com.example.caching.services.CachingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CacheEntityControllerTest {

    @Mock
    private CachingService cachingService;

    @InjectMocks
    private CacheEntityController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())  
                .build();
    }

    @Test
    void addEntity_Success() throws Exception {
        CustomEntity entity = new CustomEntity();
        entity.setId(1L);

        when(cachingService.add(any(CustomEntity.class))).thenReturn(entity);

        mockMvc.perform(post("/entities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void addEntity_Failure() throws Exception {
        when(cachingService.add(any(CustomEntity.class)))
                .thenThrow(new CacheException("Failed to add entity"));

        mockMvc.perform(post("/entities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": 1}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Cache operation failed: Failed to add entity"));
    }

    @Test
    void getEntity_Success() throws Exception {
        CustomEntity entity = new CustomEntity();
        entity.setId(1L);

        when(cachingService.get(1L)).thenReturn(Optional.of(entity));

        mockMvc.perform(get("/entities/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getEntity_NotFound() throws Exception {
        when(cachingService.get(1L))
                .thenThrow(new EntityNotFoundException("Entity with ID 1 not found"));

        mockMvc.perform(get("/entities/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Entity not found: Entity with ID 1 not found"));
    }

    @Test
    void getEntity_Failure() throws Exception {
        when(cachingService.get(1L))
                .thenThrow(new CacheException("Failed to retrieve entity"));

        mockMvc.perform(get("/entities/1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Cache operation failed: Failed to retrieve entity"));
    }

    @Test
    void removeEntity_Success() throws Exception {
        doNothing().when(cachingService).remove(1L);

        mockMvc.perform(delete("/entities/1"))
                .andExpect(status().isOk());

        verify(cachingService).remove(1L);
    }

    @Test
    void removeEntity_NotFound() throws Exception {
        doThrow(new EntityNotFoundException("Entity with ID 1 not found"))
                .when(cachingService).remove(1L);

        mockMvc.perform(delete("/entities/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Entity not found: Entity with ID 1 not found"));
    }

    @Test
    void removeEntity_Failure() throws Exception {
        doThrow(new CacheException("Failed to remove entity"))
                .when(cachingService).remove(1L);

        mockMvc.perform(delete("/entities/1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Cache operation failed: Failed to remove entity"));
    }

    @Test
    void removeAllEntities_Success() throws Exception {
        doNothing().when(cachingService).removeAll();

        mockMvc.perform(delete("/entities"))
                .andExpect(status().isOk());

        verify(cachingService).removeAll();
    }

    @Test
    void removeAllEntities_Failure() throws Exception {
        doThrow(new CacheException("Failed to remove all entities"))
                .when(cachingService).removeAll();

        mockMvc.perform(delete("/entities"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Cache operation failed: Failed to remove all entities"));
    }

    @Test
    void clearCache_Success() throws Exception {
        doNothing().when(cachingService).clear();

        mockMvc.perform(post("/entities/clear-cache"))
                .andExpect(status().isOk());

        verify(cachingService).clear();
    }

    @Test
    void clearCache_Failure() throws Exception {
        doThrow(new CacheException("Failed to clear cache"))
                .when(cachingService).clear();

        mockMvc.perform(post("/entities/clear-cache"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Cache operation failed: Failed to clear cache"));
    }
}