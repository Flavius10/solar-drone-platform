package com.solar.backend.repository;

import com.solar.backend.model.VideoInspectionBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoInspectionBatchRepository extends JpaRepository<VideoInspectionBatch, Long> {
    List<VideoInspectionBatch> findAllByOrderByCreatedAtDesc();

    List<VideoInspectionBatch> findByFarm_IdOrderByCreatedAtDesc(Long farmId);
}
