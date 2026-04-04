package com.payassure.repository;

import com.payassure.model.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, String> {
    Optional<Zone> findByName(String name);
    Optional<Zone> findByNameAndCity(String name, String city);
    List<Zone> findByCity(String city);
    List<Zone> findByActiveTrue();

    // For production: Use MySQL Spatial Extensions (ST_Contains, ST_GeomFromGeoJSON)
    // This is a placeholder for future implementation
    @Query(value = "SELECT * FROM zones WHERE active = true AND city = :city", nativeQuery = true)
    List<Zone> findActiveZonesByCity(@Param("city") String city);
}
