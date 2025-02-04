package com.example.caching.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.caching.exception.CacheException;
import com.example.caching.exception.EntityNotFoundException;
import com.example.caching.model.CustomEntity;
import com.example.caching.repository.EntityRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CachingService {

    @Autowired
    private EntityRepository entityRepository;

    @Value("${cache.max-elements}")
    private int maxElements;

    private final Map<Long, CustomEntity> cache = new LinkedHashMap<Long, CustomEntity>(maxElements, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, CustomEntity> eldest) {
            if (size() > maxElements) {
                evictToDatabase(eldest.getValue());
                return true;
            }
            return false;
        }
    };


    private void evictToDatabase(CustomEntity entity) {
        try {
            entityRepository.save(entity); 
            log.info("Evicted entity with ID {} to the database.", entity.getId());
        } catch (Exception ex) {
            log.error("Failed to evict entity with ID {} to the database: {}", entity.getId(), ex.getMessage());
            throw new CacheException("Failed to evict entity to database");
        }
    }

    @CachePut(value = "entities", key = "#entity.id")
    public CustomEntity add(CustomEntity entity) {
        log.info("Adding entity with ID: {}", entity.getId());
        try {
            CustomEntity savedEntity = entityRepository.save(entity);
            cache.put(savedEntity.getId(), savedEntity);
            return savedEntity;
        } catch (Exception ex) {
            log.error("Failed to add entity with ID: {}", entity.getId(), ex);
            throw new CacheException("Failed to add entity to cache and database");
        }
    }

    @CacheEvict(value = "entities", key = "#id")
    public void remove(Long id) {
        log.info("Removing entity with ID: {}", id);
        try {
            if (!entityRepository.existsById(id)) {
                throw new EntityNotFoundException("Entity with ID " + id + " not found");
            }
            entityRepository.deleteById(id);
            cache.remove(id);
        } catch (EntityNotFoundException ex) {
            log.error("Entity not found: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to remove entity with ID: {}", id, ex);
            throw new CacheException("Failed to remove entity from cache and database");
        }
    }

    @CacheEvict(value = "entities", allEntries = true)
    public void removeAll() {
        log.info("Removing all entities");
        try {
            entityRepository.deleteAll();
            cache.clear(); 
        } catch (Exception ex) {
            log.error("Failed to remove all entities", ex);
            throw new CacheException("Failed to remove all entities from cache and database");
        }
    }

    @Cacheable(value = "entities", key = "#id")
    public Optional<CustomEntity> get(Long id) {
        log.info("Getting entity with ID: {}", id);
        try {
            if (cache.containsKey(id)) {
                return Optional.of(cache.get(id));
            }
            Optional<CustomEntity> entity = entityRepository.findById(id);
            if (entity.isEmpty()) {
                throw new EntityNotFoundException("Entity with ID " + id + " not found");
            }
            cache.put(id, entity.get());
            return entity;
        } catch (EntityNotFoundException ex) {
            log.error("Entity not found: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to get entity with ID: {}", id, ex);
            throw new CacheException("Failed to retrieve entity from cache or database");
        }
    }

    @CacheEvict(value = "entities", allEntries = true)
    public void clear() {
        log.info("Clearing cache");
        try {
            cache.clear();
        } catch (Exception ex) {
            log.error("Failed to clear cache", ex);
            throw new CacheException("Failed to clear cache");
        }
    }

    public List<CustomEntity> getAll() {
        log.info("Getting all entities");
        try {
            List<CustomEntity> entities = entityRepository.findAll();
            if (entities.isEmpty()) {
                throw new EntityNotFoundException("No entities found");
            }
            for (CustomEntity entity : entities) {
                cache.put(entity.getId(), entity);
            }
            return entities;
        } catch (EntityNotFoundException ex) {
            log.error("No entities found: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to get all entities", ex);
            throw new CacheException("Failed to retrieve all entities from database");
        }
    }
}