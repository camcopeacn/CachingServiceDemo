package com.example.caching.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import com.example.caching.exception.CacheException;
import com.example.caching.exception.EntityNotFoundException;
import com.example.caching.model.CustomEntity;
import com.example.caching.repository.EntityRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
public class CachingServiceTest {

    @Mock
    private EntityRepository entityRepository;

    @InjectMocks
    private CachingService cachingService;

    private CustomEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new CustomEntity();
        testEntity.setId(1L);
        testEntity.setData("Test Data");
    }

    @Test
    void testAdd_Success() {
        when(entityRepository.save(any(CustomEntity.class))).thenReturn(testEntity);
    
        CustomEntity result = cachingService.add(testEntity);

        assertNotNull(result, "The result should not be null");
        assertEquals(testEntity.getId(), result.getId(), "Entity ID should match");
        assertEquals(testEntity.getData(), result.getData(), "Entity data should match");

        verify(entityRepository, atLeastOnce()).save(any(CustomEntity.class));
    }
    
    @Test
    void testAdd_Failure() {

        when(entityRepository.save(any(CustomEntity.class)))
                .thenThrow(new RuntimeException("Database error"));

        CacheException exception = assertThrows(CacheException.class, () -> {
            cachingService.add(testEntity);
        });

        assertEquals("Failed to add entity to cache and database", exception.getMessage());
        verify(entityRepository).save(testEntity);
    }

    @Test
    void testGet_Success() {

        when(entityRepository.findById(1L)).thenReturn(Optional.of(testEntity));

        Optional<CustomEntity> result = cachingService.get(1L);

        assertTrue(result.isPresent());
        assertEquals(testEntity.getId(), result.get().getId());
        assertEquals(testEntity.getData(), result.get().getData());
        verify(entityRepository).findById(1L);
    }

    @Test
    void testGet_EntityNotFound() {

        when(entityRepository.findById(1L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            cachingService.get(1L);
        });

        assertEquals("Entity with ID 1 not found", exception.getMessage());
        verify(entityRepository).findById(1L);
    }

    @Test
    void testGet_Failure() {

        when(entityRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        CacheException exception = assertThrows(CacheException.class, () -> {
            cachingService.get(1L);
        });

        assertEquals("Failed to retrieve entity from cache or database", exception.getMessage());
        verify(entityRepository).findById(1L);
    }

    @Test
    void testRemove_Success() {

        when(entityRepository.existsById(1L)).thenReturn(true);
        doNothing().when(entityRepository).deleteById(1L);

        cachingService.remove(1L);

        verify(entityRepository).deleteById(1L);
    }

    @Test
    void testRemove_EntityNotFound() {

        when(entityRepository.existsById(1L)).thenReturn(false);

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            cachingService.remove(1L);
        });

        assertEquals("Entity with ID 1 not found", exception.getMessage());
        verify(entityRepository).existsById(1L);
    }

    @Test
    void testRemove_Failure() {

        when(entityRepository.existsById(1L)).thenReturn(true);
        doThrow(new RuntimeException("Database error")).when(entityRepository).deleteById(1L);

        CacheException exception = assertThrows(CacheException.class, () -> {
            cachingService.remove(1L);
        });

        assertEquals("Failed to remove entity from cache and database", exception.getMessage());
        verify(entityRepository).deleteById(1L);
    }

    @Test
    void testRemoveAll_Success() {

        doNothing().when(entityRepository).deleteAll();

        cachingService.removeAll();

        verify(entityRepository).deleteAll();
    }

    @Test
    void testRemoveAll_Failure() {

        doThrow(new RuntimeException("Database error")).when(entityRepository).deleteAll();

        CacheException exception = assertThrows(CacheException.class, () -> {
            cachingService.removeAll();
        });

        assertEquals("Failed to remove all entities from cache and database",
                exception.getMessage());
        verify(entityRepository).deleteAll();
    }

    @Test
    void testClear_Success() {
        assertDoesNotThrow(() -> cachingService.clear());
    }

    @Test
    void testClear_Failure() throws Exception {
        // reflect field
        Field cacheField = CachingService.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        LinkedHashMap<Long, Object> cacheSpy = spy(new LinkedHashMap<>());
        cacheField.set(cachingService, cacheSpy);

        // throw eeror
        doThrow(new RuntimeException("Cache error")).when(cacheSpy).clear();

        CacheException exception = assertThrows(CacheException.class, () -> {
            cachingService.clear();
        });

        assertEquals("Failed to clear cache", exception.getMessage());
    }


    @Test
    void testGetAll_Success() {

        List<CustomEntity> entities = Arrays.asList(testEntity);
        when(entityRepository.findAll()).thenReturn(entities);

        List<CustomEntity> result = cachingService.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testEntity.getId(), result.get(0).getId());
        assertEquals(testEntity.getData(), result.get(0).getData());
        verify(entityRepository).findAll();
    }

    @Test
    void testGetAll_NoEntitiesFound() {

        when(entityRepository.findAll()).thenReturn(List.of());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            cachingService.getAll();
        });

        assertEquals("No entities found", exception.getMessage());
        verify(entityRepository).findAll();
    }

    @Test
    void testGetAll_Failure() {

        when(entityRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        CacheException exception = assertThrows(CacheException.class, () -> {
            cachingService.getAll();
        });

        assertEquals("Failed to retrieve all entities from database", exception.getMessage());
        verify(entityRepository).findAll();
    }

    @Test
    void testEvictToDatabase_Success() throws Exception {
        // crying bc private method
        when(entityRepository.save(testEntity)).thenReturn(testEntity);


        Method evictToDatabaseMethod =
                CachingService.class.getDeclaredMethod("evictToDatabase", CustomEntity.class);

        evictToDatabaseMethod.setAccessible(true);

        evictToDatabaseMethod.invoke(cachingService, testEntity);
        verify(entityRepository).save(testEntity);

    }

    @Test
    void testEvictToDatabase_Failure() {

        when(entityRepository.save(any(CustomEntity.class)))
                .thenThrow(new RuntimeException("Database error"));

        CacheException exception = assertThrows(CacheException.class, () -> {
            cachingService.add(testEntity);
        });

        assertEquals("Failed to add entity to cache and database", exception.getMessage());
        verify(entityRepository).save(testEntity);
    }
}
