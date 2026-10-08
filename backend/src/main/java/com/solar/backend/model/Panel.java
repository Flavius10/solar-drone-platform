package com.solar.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "panels", uniqueConstraints = @UniqueConstraint(columnNames = {"farm_id", "panel_row", "panel_column"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Panel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @Column(name = "panel_row", nullable = false)
    private Integer rowNumber;

    @Column(name = "panel_column", nullable = false)
    private Integer columnNumber;

    private String label;

    
    
    
    
    
    
    @Column(name = "wattage")
    private Integer wattage;

    @Enumerated(EnumType.STRING)
    @Column(name = "panel_tier")
    private PanelTier tier;

    @Transient
    public int getEffectiveWattage() {
        if (wattage != null) return wattage;
        if (farm != null && farm.getDefaultWattage() != null) return farm.getDefaultWattage();
        return 400;
    }

    @Transient
    public PanelTier getEffectiveTier() {
        if (tier != null) return tier;
        if (farm != null && farm.getDefaultTier() != null) return farm.getDefaultTier();
        return PanelTier.STANDARD;
    }
}