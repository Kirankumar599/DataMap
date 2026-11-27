package com.datamap.repository;

import com.datamap.entity.ColumnFindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for column findings metadata.
 */
@Repository
public interface ColumnFindingRepository extends JpaRepository<ColumnFindingEntity, UUID> {
    List<ColumnFindingEntity> findByScanId(UUID scanId);
}
