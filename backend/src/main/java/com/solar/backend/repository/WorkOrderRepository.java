package com.solar.backend.repository;

import com.solar.backend.model.Panel;
import com.solar.backend.model.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findByPanel(Panel panel);

    List<WorkOrder> findByAssignedTo(String assignedTo);

    List<WorkOrder> findByStatus(String status);
}
