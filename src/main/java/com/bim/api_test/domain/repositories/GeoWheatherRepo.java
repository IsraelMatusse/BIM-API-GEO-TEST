package com.bim.api_test.domain.repositories;

import com.bim.api_test.domain.entities.GeoWhetherHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;


public interface GeoWheatherRepo extends JpaRepository<GeoWhetherHistory, String>, JpaSpecificationExecutor<GeoWhetherHistory> {

    Optional<GeoWhetherHistory> findByIdAndDeletedAtIsNull(String id);

}
