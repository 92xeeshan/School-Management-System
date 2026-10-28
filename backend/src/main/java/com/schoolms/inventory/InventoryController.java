package com.schoolms.inventory;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.common.api.PagedResponse;
import com.schoolms.inventory.dto.InventoryAssigneeDto;
import com.schoolms.inventory.dto.InventoryCategoryDto;
import com.schoolms.inventory.dto.InventoryCategoryRequest;
import com.schoolms.inventory.dto.InventoryItemDto;
import com.schoolms.inventory.dto.InventoryItemRequest;
import com.schoolms.inventory.dto.InventoryTransactionDto;
import com.schoolms.inventory.dto.InventoryTransactionRequest;
import com.schoolms.student.dto.ImportResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Inventory")
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "List inventory categories")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    @GetMapping("/categories")
    public ApiResponse<List<InventoryCategoryDto>> listCategories() {
        return ApiResponse.ok(inventoryService.listCategories());
    }

    @Operation(summary = "Create an inventory category")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    @PostMapping("/categories")
    public ApiResponse<InventoryCategoryDto> createCategory(@Valid @RequestBody InventoryCategoryRequest request) {
        return ApiResponse.ok(inventoryService.createCategory(request), "inventory.category_created");
    }

    @Operation(summary = "Delete an inventory category")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable UUID id) {
        inventoryService.deleteCategory(id);
        return ApiResponse.ok(null, "inventory.category_deleted");
    }

    @Operation(summary = "List staff that assets can be assigned to")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    @GetMapping("/assignees")
    public ApiResponse<List<InventoryAssigneeDto>> listAssignees() {
        return ApiResponse.ok(inventoryService.listAssignees());
    }

    @Operation(summary = "List inventory items (paginated, filterable and sortable)")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    @GetMapping("/items")
    public ApiResponse<PagedResponse<InventoryItemDto>> listItems(
            @RequestParam(required = false) UUID category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {
        return ApiResponse.ok(inventoryService.listItems(
                category, location, condition, lowStock, query, page, size, sort, direction));
    }

    @Operation(summary = "Get an inventory item")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    @GetMapping("/items/{id}")
    public ApiResponse<InventoryItemDto> getItem(@PathVariable UUID id) {
        return ApiResponse.ok(inventoryService.getItem(id));
    }

    @Operation(summary = "Create an inventory item")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    @PostMapping("/items")
    public ApiResponse<InventoryItemDto> createItem(@Valid @RequestBody InventoryItemRequest request) {
        return ApiResponse.ok(inventoryService.createItem(request), "inventory.created");
    }

    @Operation(summary = "Update an inventory item")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    @PutMapping("/items/{id}")
    public ApiResponse<InventoryItemDto> updateItem(
            @PathVariable UUID id,
            @Valid @RequestBody InventoryItemRequest request) {
        return ApiResponse.ok(inventoryService.updateItem(id, request), "inventory.updated");
    }

    @Operation(summary = "Delete an inventory item")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    @DeleteMapping("/items/{id}")
    public ApiResponse<Void> deleteItem(@PathVariable UUID id) {
        inventoryService.deleteItem(id);
        return ApiResponse.ok(null, "inventory.deleted");
    }

    @Operation(summary = "Record a stock or checkout transaction")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    @PostMapping("/items/{id}/transactions")
    public ApiResponse<InventoryItemDto> recordTransaction(
            @PathVariable UUID id,
            @Valid @RequestBody InventoryTransactionRequest request) {
        return ApiResponse.ok(inventoryService.recordTransaction(id, request), "inventory.transaction_recorded");
    }

    @Operation(summary = "List item transactions")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    @GetMapping("/items/{id}/transactions")
    public ApiResponse<List<InventoryTransactionDto>> listTransactions(@PathVariable UUID id) {
        return ApiResponse.ok(inventoryService.listTransactions(id));
    }

    @Operation(summary = "Bulk import inventory items from CSV")
    @PreAuthorize("hasAuthority('INVENTORY_IMPORT')")
    @PostMapping("/import")
    public ApiResponse<ImportResult> importCsv(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(inventoryService.importCsv(file), "inventory.imported");
    }

    @Operation(summary = "Export inventory report as PDF")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    @GetMapping("/report")
    public ResponseEntity<byte[]> report(
            @RequestParam(required = false) UUID category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) Boolean lowStock) {
        byte[] pdf = inventoryService.exportReport(category, location, condition, lowStock);
        return ResponseEntity.ok().headers(inventoryService.reportHeaders()).body(pdf);
    }
}
