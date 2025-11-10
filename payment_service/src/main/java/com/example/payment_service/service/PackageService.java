package com.example.payment_service.service;

import com.example.payment_service.dto.request.CreatePackageRequest;
import com.example.payment_service.dto.request.UpdatePackageRequest;
import com.example.payment_service.dto.response.PackageResponse;
import com.example.payment_service.entity.Package;
import com.example.payment_service.repository.PackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PackageService {

    private final PackageRepository packageRepository;

    /**
     * Create a new package
     */
    public PackageResponse createPackage(CreatePackageRequest request) {
        log.info("Creating new package with code: {}", request.getCode());

        // Check if code already exists
        if (packageRepository.findByCode(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("Package with code " + request.getCode() + " already exists");
        }

        Package pkg = Package.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .credits(request.getCredits())
                .bonusCredits(request.getBonusCredits() != null ? request.getBonusCredits() : 0)
                .price(request.getPrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .metadata(request.getMetadata())
                .build();

        Package savedPackage = packageRepository.save(pkg);
        log.info("Package created successfully with id: {}", savedPackage.getId());

        return mapToResponse(savedPackage);
    }

    /**
     * Get package by ID
     */
    @Transactional(readOnly = true)
    public PackageResponse getPackageById(Long id) {
        log.info("Getting package by id: {}", id);

        Package pkg = packageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Package not found with id: " + id));

        return mapToResponse(pkg);
    }

    /**
     * Get package by code
     */
    @Transactional(readOnly = true)
    public PackageResponse getPackageByCode(String code) {
        log.info("Getting package by code: {}", code);

        Package pkg = packageRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Package not found with code: " + code));

        return mapToResponse(pkg);
    }

    /**
     * Get all packages
     */
    @Transactional(readOnly = true)
    public List<PackageResponse> getAllPackages() {
        log.info("Getting all packages");

        return packageRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all active packages
     */
    @Transactional(readOnly = true)
    public List<PackageResponse> getActivePackages() {
        log.info("Getting all active packages");

        return packageRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update package
     */
    public PackageResponse updatePackage(Long id, UpdatePackageRequest request) {
        log.info("Updating package with id: {}", id);

        Package pkg = packageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Package not found with id: " + id));

        // Update only non-null fields
        if (request.getName() != null) {
            pkg.setName(request.getName());
        }
        if (request.getDescription() != null) {
            pkg.setDescription(request.getDescription());
        }
        if (request.getCredits() != null) {
            pkg.setCredits(request.getCredits());
        }
        if (request.getBonusCredits() != null) {
            pkg.setBonusCredits(request.getBonusCredits());
        }
        if (request.getPrice() != null) {
            pkg.setPrice(request.getPrice());
        }
        if (request.getCurrency() != null) {
            pkg.setCurrency(request.getCurrency());
        }
        if (request.getIsActive() != null) {
            pkg.setIsActive(request.getIsActive());
        }
        if (request.getSortOrder() != null) {
            pkg.setSortOrder(request.getSortOrder());
        }
        if (request.getMetadata() != null) {
            pkg.setMetadata(request.getMetadata());
        }

        Package updatedPackage = packageRepository.save(pkg);
        log.info("Package updated successfully with id: {}", updatedPackage.getId());

        return mapToResponse(updatedPackage);
    }

    /**
     * Delete package
     */
    public void deletePackage(Long id) {
        log.info("Deleting package with id: {}", id);

        Package pkg = packageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Package not found with id: " + id));

        packageRepository.delete(pkg);
        log.info("Package deleted successfully with id: {}", id);
    }

    /**
     * Activate/Deactivate package
     */
    public PackageResponse togglePackageStatus(Long id) {
        log.info("Toggling package status for id: {}", id);

        Package pkg = packageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Package not found with id: " + id));

        pkg.setIsActive(!pkg.getIsActive());
        Package updatedPackage = packageRepository.save(pkg);

        log.info("Package status toggled to: {} for id: {}", updatedPackage.getIsActive(), id);

        return mapToResponse(updatedPackage);
    }

    /**
     * Map Package entity to PackageResponse DTO
     */
    private PackageResponse mapToResponse(Package pkg) {
        return PackageResponse.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .code(pkg.getCode())
                .description(pkg.getDescription())
                .credits(pkg.getCredits())
                .bonusCredits(pkg.getBonusCredits())
                .price(pkg.getPrice())
                .currency(pkg.getCurrency())
                .isActive(pkg.getIsActive())
                .sortOrder(pkg.getSortOrder())
                .metadata(pkg.getMetadata())
                .createdAt(pkg.getCreatedAt())
                .updatedAt(pkg.getUpdatedAt())
                .build();
    }
}

