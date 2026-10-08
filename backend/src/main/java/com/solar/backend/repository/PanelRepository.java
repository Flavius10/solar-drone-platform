package com.solar.backend.repository;

import com.solar.backend.model.Farm;
import com.solar.backend.model.Panel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PanelRepository extends JpaRepository<Panel, Long> {
    Panel findByRowNumberAndColumnNumber(Integer rowNumber, Integer columnNumber);

    Panel findByFarmAndRowNumberAndColumnNumber(Farm farm, Integer rowNumber, Integer columnNumber);

    List<Panel> findByFarm(Farm farm);

    List<Panel> findByFarmId(Long farmId);
}