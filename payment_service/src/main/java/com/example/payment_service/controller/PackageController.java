package com.example.payment_service.controller;

import com.example.payment_service.dto.ApiResponse;
import com.example.payment_service.dto.request.CreatePackageRequest;
import com.example.payment_service.dto.request.UpdatePackageRequest;
import com.example.payment_service.dto.response.PackageResponse;
import com.example.payment_service.service.PackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for package management operations
 * Handles CRUD operations for credit packages
 */
@RestController
@RequestMapping("/api/payments/packages")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Packages", description = "Package management APIs for credit packages")
public class PackageController {

    private final PackageService packageService;

    @PostMapping
    @Operation(summary = "Create new package", description = "Create a new credit package")
    public ResponseEntity<ApiResponse<PackageResponse>> createPackage(
            @Valid @RequestBody CreatePackageRequest request
    ) {
        log.info("Creating new package with code: {}", request.getCode());

        try {
            PackageResponse response = packageService.createPackage(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<PackageResponse>builder()
                            .code(1000)
                            .message("Package created successfully")
                            .result(response)
                            .build());
        } catch (IllegalArgumentException e) {
            log.error("Failed to create package: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<PackageResponse>builder()
                            .code(1001)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error creating package", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PackageResponse>builder()
                            .code(9999)
                            .message("An error occurred while creating package")
                            .build());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get package by ID", description = "Retrieve package details by ID")
    public ResponseEntity<ApiResponse<PackageResponse>> getPackageById(
            @Parameter(description = "Package ID")
            @PathVariable Long id
    ) {
        log.info("Getting package by id: {}", id);

        try {
            PackageResponse response = packageService.getPackageById(id);
            return ResponseEntity.ok(ApiResponse.<PackageResponse>builder()
                    .code(1000)
                    .message("Package retrieved successfully")
                    .result(response)
                    .build());
        } catch (RuntimeException e) {
            log.error("Package not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<PackageResponse>builder()
                            .code(1002)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error getting package by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PackageResponse>builder()
                            .code(9999)
                            .message("An error occurred while retrieving package")
                            .build());
        }
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get package by code", description = "Retrieve package details by code")
    public ResponseEntity<ApiResponse<PackageResponse>> getPackageByCode(
            @Parameter(description = "Package code")
            @PathVariable String code
    ) {
        log.info("Getting package by code: {}", code);

        try {
            PackageResponse response = packageService.getPackageByCode(code);
            return ResponseEntity.ok(ApiResponse.<PackageResponse>builder()
                    .code(1000)
                    .message("Package retrieved successfully")
                    .result(response)
                    .build());
        } catch (RuntimeException e) {
            log.error("Package not found with code: {}", code);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<PackageResponse>builder()
                            .code(1002)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error getting package by code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PackageResponse>builder()
                            .code(9999)
                            .message("An error occurred while retrieving package")
                            .build());
        }
    }

    @GetMapping
    @Operation(summary = "Get all packages", description = "Retrieve all credit packages")
    public ResponseEntity<ApiResponse<List<PackageResponse>>> getAllPackages() {
        log.info("Getting all packages");

        try {
            List<PackageResponse> responses = packageService.getAllPackages();
            return ResponseEntity.ok(ApiResponse.<List<PackageResponse>>builder()
                    .code(1000)
                    .message("Packages retrieved successfully")
                    .result(responses)
                    .build());
        } catch (Exception e) {
            log.error("Error getting all packages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<PackageResponse>>builder()
                            .code(9999)
                            .message("An error occurred while retrieving packages")
                            .build());
        }
    }

    @GetMapping("/active")
    @Operation(summary = "Get active packages", description = "Retrieve all active credit packages ordered by sort order")
    public ResponseEntity<ApiResponse<List<PackageResponse>>> getActivePackages() {
        log.info("Getting all active packages");

        try {
            List<PackageResponse> responses = packageService.getActivePackages();
            return ResponseEntity.ok(ApiResponse.<List<PackageResponse>>builder()
                    .code(1000)
                    .message("Active packages retrieved successfully")
                    .result(responses)
                    .build());
        } catch (Exception e) {
            log.error("Error getting active packages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<PackageResponse>>builder()
                            .code(9999)
                            .message("An error occurred while retrieving active packages")
                            .build());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update package", description = "Update an existing package")
    public ResponseEntity<ApiResponse<PackageResponse>> updatePackage(
            @Parameter(description = "Package ID")
            @PathVariable Long id,
            @Valid @RequestBody UpdatePackageRequest request
    ) {
        log.info("Updating package with id: {}", id);

        try {
            PackageResponse response = packageService.updatePackage(id, request);
            return ResponseEntity.ok(ApiResponse.<PackageResponse>builder()
                    .code(1000)
                    .message("Package updated successfully")
                    .result(response)
                    .build());
        } catch (RuntimeException e) {
            log.error("Package not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<PackageResponse>builder()
                            .code(1002)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error updating package", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PackageResponse>builder()
                            .code(9999)
                            .message("An error occurred while updating package")
                            .build());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete package", description = "Delete a package by ID")
    public ResponseEntity<ApiResponse<Void>> deletePackage(
            @Parameter(description = "Package ID")
            @PathVariable Long id
    ) {
        log.info("Deleting package with id: {}", id);

        try {
            packageService.deletePackage(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .code(1000)
                    .message("Package deleted successfully")
                    .build());
        } catch (RuntimeException e) {
            log.error("Package not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>builder()
                            .code(1002)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error deleting package", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>builder()
                            .code(9999)
                            .message("An error occurred while deleting package")
                            .build());
        }
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Toggle package status", description = "Activate or deactivate a package")
    public ResponseEntity<ApiResponse<PackageResponse>> togglePackageStatus(
            @Parameter(description = "Package ID")
            @PathVariable Long id
    ) {
        log.info("Toggling package status for id: {}", id);

        try {
            PackageResponse response = packageService.togglePackageStatus(id);
            return ResponseEntity.ok(ApiResponse.<PackageResponse>builder()
                    .code(1000)
                    .message("Package status updated successfully")
                    .result(response)
                    .build());
        } catch (RuntimeException e) {
            log.error("Package not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<PackageResponse>builder()
                            .code(1002)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error toggling package status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PackageResponse>builder()
                            .code(9999)
                            .message("An error occurred while toggling package status")
                            .build());
        }
    }
}

