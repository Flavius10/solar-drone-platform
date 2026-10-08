package com.solar.backend.repository;

import com.solar.backend.model.Inspection;
import com.solar.backend.model.Panel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    long countByStatus(String status);

    @Query("SELECT SUM(i.estimatedRepairCost) FROM Inspection i")
    Double sumEstimatedRepairCost();

    Optional<Inspection> findTopByPanelOrderByIdDesc(Panel panel);

    java.util.List<Inspection> findByPanelOrderByInspectionDateAscIdAsc(Panel panel);

    java.util.List<Inspection> findByPanel_FarmId(Long farmId);

    java.util.List<Inspection> findByVideoBatchIdOrderByFrameIndexAsc(Long videoBatchId);
}