package com.datamap.repository;

import com.datamap.entity.ScanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for scan audit metadata.
 */
@Repository
public interface ScanRepository extends JpaRepository<ScanEntity, UUID> {
    List<ScanEntity> findAllByOrderByCreatedAtDesc();
}
