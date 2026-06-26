package com.cafequeue.inventory.interfaces;

import com.cafequeue.common.contract.InventoryReservationRequest;
import com.cafequeue.common.contract.InventoryReservationResponse;
import com.cafequeue.common.core.ApiResponse;
import com.cafequeue.inventory.application.InventoryApplicationService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class RecipeInventoryController {
    private final InventoryApplicationService inventoryService;

    public RecipeInventoryController(InventoryApplicationService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/api/recipes")
    public ApiResponse<List<InventoryApplicationService.RecipeView>> recipes() {
        return ApiResponse.ok(inventoryService.recipes());
    }

    @GetMapping("/api/inventory/machines/{machineId}")
    public ApiResponse<Map<String, Integer>> machineInventory(@PathVariable String machineId) {
        return ApiResponse.ok(inventoryService.machineInventory(machineId));
    }

    @PostMapping("/api/inventory/reservations")
    public ApiResponse<InventoryReservationResponse> reserve(@RequestBody InventoryReservationRequest request) {
        return ApiResponse.ok(inventoryService.reserve(request));
    }

    @PostMapping("/api/inventory/reservations/{reservationId}/commit")
    public ApiResponse<InventoryReservationResponse> commit(@PathVariable String reservationId) {
        return ApiResponse.ok(inventoryService.commit(reservationId));
    }

    @PostMapping("/api/inventory/reservations/{reservationId}/release")
    public ApiResponse<InventoryReservationResponse> release(@PathVariable String reservationId) {
        return ApiResponse.ok(inventoryService.release(reservationId));
    }
}
