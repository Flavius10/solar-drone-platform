package com.solar.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "farms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Farm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String location;

    private LocalDateTime createdAt;

    
    
    
    @Column(name = "default_wattage")
    private Integer defaultWattage = 400;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_tier")
    private PanelTier defaultTier = PanelTier.STANDARD;

    
    
    
    @Column(name = "critical_confidence_threshold")
    private Double criticalConfidenceThreshold = 0.7;

    
    
    
    
    @Column(name = "critical_defect_types")
    private String criticalDefectTypesCsv = "HOTSPOT,CRACK,PID_EFFECT,DIODE_FAILURE";

    @Transient
    public Set<String> getCriticalDefectTypes() {
        if (criticalDefectTypesCsv == null || criticalDefectTypesCsv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(criticalDefectTypesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void setCriticalDefectTypes(Set<String> types) {
        this.criticalDefectTypesCsv = (types == null || types.isEmpty())
                ? "" : String.join(",", types);
    }

    
    
    @Column(name = "webhook_url")
    private String webhookUrl;

    
    
    
    @Enumerated(EnumType.STRING)
    @Column(name = "currency")
    private Currency currency = Currency.USD;

    
    
    
    
    @Column(name = "labor_rate_per_hour")
    private Double laborRatePerHour;

    
    
    
    @Column(name = "report_company_name")
    private String reportCompanyName;

    @Column(name = "report_accent_color")
    private String reportAccentColor;

    
    
    @Column(name = "report_logo_path")
    private String reportLogoPath;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
