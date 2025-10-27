package com.example.payment_service.repository;

import com.example.payment_service.entity.Package;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PackageRepository extends JpaRepository<Package, Long> {
    Optional<Package> findByCode(String code);
    List<Package> findByIsActiveTrueOrderBySortOrderAsc();
}
