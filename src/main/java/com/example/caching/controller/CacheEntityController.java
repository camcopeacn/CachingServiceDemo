package com.example.caching.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.caching.exception.CacheException;
import com.example.caching.exception.EntityNotFoundException;
import com.example.caching.model.CustomEntity;
import com.example.caching.services.CachingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/entities")
@RequiredArgsConstructor
public class CacheEntityController {

    @Autowired
    private CachingService cachingService;

    @PostMapping
    public CustomEntity add(@RequestBody CustomEntity entity) {
        try {
            return cachingService.add(entity);
        } catch (CacheException ex) {
            throw ex; 
        } catch (Exception ex) {
            throw new CacheException("Failed to add entity: " + ex.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public void remove(@PathVariable Long id) {
        try {
            cachingService.remove(id);
        } catch (EntityNotFoundException ex) {
            throw ex; 
        } catch (CacheException ex) {
            throw ex; 
        } catch (Exception ex) {
            throw new CacheException("Failed to remove entity: " + ex.getMessage());
        }
    }

    @DeleteMapping
    public void removeAll() {
        try {
            cachingService.removeAll();
        } catch (CacheException ex) {
            throw ex; 
        } catch (Exception ex) {
            throw new CacheException("Failed to remove all entities: " + ex.getMessage());
        }
    }

    @GetMapping("/get/{id}")
    public Optional<CustomEntity> get(@PathVariable Long id) {
        try {
            Optional<CustomEntity> entity = cachingService.get(id);
            if (entity.isEmpty()) {
                throw new EntityNotFoundException("Entity with ID " + id + " not found");
            }
            return entity;
        } catch (EntityNotFoundException ex) {
            throw ex; 
        } catch (CacheException ex) {
            throw ex; 
        } catch (Exception ex) {
            throw new CacheException("Failed to retrieve entity: " + ex.getMessage());
        }
    }

    @PostMapping("/clear-cache")
    public void clearCache() {
        try {
            cachingService.clear();
        } catch (CacheException ex) {
            throw ex; 
        } catch (Exception ex) {
            throw new CacheException("Failed to clear cache: " + ex.getMessage());
        }
    }
}