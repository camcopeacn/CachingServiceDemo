package com.example.caching.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.caching.model.CustomEntity;

@Repository
public interface EntityRepository extends JpaRepository<CustomEntity, Long> {

    
}
