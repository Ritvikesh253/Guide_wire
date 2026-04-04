package com.payassure.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.MySQL8Dialect;

@Entity
@Table(name = "zones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private double riskIndex;      // 0.5 (safe) to 2.0 (high-risk)

    @Column(nullable = false)
    private double baseWeeklyPremium;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String geoJsonPolygon;  // GeoJSON polygon coordinates

    private double centerLat;
    private double centerLon;

    @Column(nullable = false)
    private boolean active = true;

    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
