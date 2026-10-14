package com.schoolms.inventory;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import com.schoolms.common.enums.InventoryCondition;
import com.schoolms.common.enums.InventoryTransactionType;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.inventory.dto.InventoryAssigneeDto;
import com.schoolms.inventory.dto.InventoryCategoryDto;
import com.schoolms.inventory.dto.InventoryCategoryRequest;
import com.schoolms.inventory.dto.InventoryItemDto;
import com.schoolms.inventory.dto.InventoryItemRequest;
import com.schoolms.inventory.dto.InventoryTransactionDto;
import com.schoolms.inventory.dto.InventoryTransactionRequest;
import com.schoolms.security.SecurityUtils;
import com.schoolms.student.dto.ImportResult;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryCategoryRepository categoryRepository;
    private final InventoryItemRepository itemRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final InventoryPdfService inventoryPdfService;

    @Transactional(readOnly = true)
    public List<InventoryCategoryDto> listCategories() {
        return categoryRepository.findBySchoolIdOrderByNameAsc(SecurityUtils.currentSchoolId())
                .stream().map(InventoryCategoryDto::from).toList();
    }

    @Transactional
    public InventoryCategoryDto createCategory(InventoryCategoryRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        String name = request.name().trim();
        if (categoryRepository.existsBySchoolIdAndNameIgnoreCase(schoolId, name)) {
            throw new BusinessException("error.conflict");
        }
        InventoryCategory category = new InventoryCategory();
        category.setSchoolId(schoolId);
        category.setName(name);
        category.setDescription(blankToNull(request.description()));
        return InventoryCategoryDto.from(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        InventoryCategory category = categoryRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("inventory_category", id));
        List<InventoryItem> items = itemRepository.findBySchoolIdOrderByNameAsc(schoolId).stream()
                .filter(item -> category.getId().equals(item.getCategoryId()))
                .toList();
        if (!items.isEmpty()) {
            throw new BusinessException("inventory.category_in_use");
        }
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public List<InventoryItemDto> listItems(UUID categoryId, String location, String condition, Boolean lowStock) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Map<UUID, InventoryCategory> categories = categoriesById(schoolId);
        Map<UUID, String> names = userNames(schoolId);
        InventoryCondition parsedCondition = parseCondition(condition);
        return itemRepository.findBySchoolIdOrderByNameAsc(schoolId).stream()
                .filter(item -> categoryId == null || categoryId.equals(item.getCategoryId()))
                .filter(item -> location == null || location.isBlank()
                        || (item.getLocation() != null && item.getLocation().equalsIgnoreCase(location.trim())))
                .filter(item -> parsedCondition == null || parsedCondition == item.getCondition())
                .filter(item -> lowStock == null || !lowStock || item.isLowStock())
                .map(item -> toItemDto(item, categories.get(item.getCategoryId()), names))
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryItemDto getItem(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        InventoryItem item = itemRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("inventory_item", id));
        InventoryCategory category = categoryRepository.findByIdAndSchoolId(item.getCategoryId(), schoolId)
                .orElse(null);
        return toItemDto(item, category, userNames(schoolId));
    }

    @Transactional
    public InventoryItemDto createItem(InventoryItemRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        InventoryCategory category = requireCategory(request.categoryId(), schoolId);
        String sku = normalizeSku(request.sku());
        if (sku != null && itemRepository.existsBySchoolIdAndSku(schoolId, sku)) {
            throw new BusinessException("inventory.sku_exists");
        }
        InventoryItem item = new InventoryItem();
        item.setSchoolId(schoolId);
        applyItemFields(item, request, category.getId(), sku);
        item.setCreatedBy(SecurityUtils.currentUserId());
        InventoryItem saved = itemRepository.save(item);
        if (saved.getQuantity() > 0) {
            recordTxn(saved, InventoryTransactionType.IN, saved.getQuantity(), "Opening stock",
                    null, saved.getLocation(), saved.getPurchaseDate() == null ? LocalDate.now() : saved.getPurchaseDate());
        }
        return toItemDto(saved, category, userNames(schoolId));
    }

    @Transactional
    public InventoryItemDto updateItem(UUID id, InventoryItemRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        InventoryItem item = itemRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("inventory_item", id));
        InventoryCategory category = requireCategory(request.categoryId(), schoolId);
        String sku = normalizeSku(request.sku());
        if (sku != null && !sku.equals(item.getSku()) && itemRepository.existsBySchoolIdAndSku(schoolId, sku)) {
            throw new BusinessException("inventory.sku_exists");
        }
        applyItemFields(item, request, category.getId(), sku);
        return toItemDto(itemRepository.save(item), category, userNames(schoolId));
    }

    @Transactional
    public void deleteItem(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        InventoryItem item = itemRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("inventory_item", id));
        itemRepository.delete(item);
    }

    @Transactional
    public InventoryItemDto recordTransaction(UUID itemId, InventoryTransactionRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        InventoryItem item = itemRepository.findByIdAndSchoolId(itemId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("inventory_item", itemId));
        InventoryTransactionType type = request.type();
        int qty = request.quantity() == null ? 0 : request.quantity();
        LocalDate date = request.transactionDate() == null ? LocalDate.now() : request.transactionDate();
        UUID assignee = request.assignedToUserId();
        if (assignee != null) {
            userRepository.findByIdAndSchoolId(assignee, schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("user", assignee));
        }

        switch (type) {
            case IN -> {
                if (qty <= 0) {
                    throw new BusinessException("inventory.quantity_required");
                }
                item.setQuantity(item.getQuantity() + qty);
                if (request.room() != null && !request.room().isBlank()) {
                    item.setLocation(request.room().trim());
                }
            }
            case OUT -> {
                if (qty <= 0) {
                    throw new BusinessException("inventory.quantity_required");
                }
                if (item.getQuantity() < qty) {
                    throw new BusinessException("inventory.insufficient_stock");
                }
                item.setQuantity(item.getQuantity() - qty);
            }
            case CHECKOUT -> {
                if (item.getAssignedToUserId() != null || (item.getAssignedRoom() != null && !item.getAssignedRoom().isBlank())) {
                    throw new BusinessException("inventory.already_checked_out");
                }
                item.setAssignedToUserId(assignee);
                item.setAssignedRoom(blankToNull(request.room()));
            }
            case CHECKIN -> {
                if (item.getAssignedToUserId() == null
                        && (item.getAssignedRoom() == null || item.getAssignedRoom().isBlank())) {
                    throw new BusinessException("inventory.not_checked_out");
                }
                item.setAssignedToUserId(null);
                item.setAssignedRoom(null);
            }
        }
        recordTxn(item, type, qty, blankToNull(request.reason()), assignee, blankToNull(request.room()), date);
        InventoryCategory category = categoryRepository.findByIdAndSchoolId(item.getCategoryId(), schoolId).orElse(null);
        return toItemDto(itemRepository.save(item), category, userNames(schoolId));
    }

    @Transactional(readOnly = true)
    public List<InventoryTransactionDto> listTransactions(UUID itemId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        itemRepository.findByIdAndSchoolId(itemId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("inventory_item", itemId));
        Map<UUID, String> names = userNames(schoolId);
        return transactionRepository
                .findBySchoolIdAndItemIdOrderByTransactionDateDescCreatedAtDesc(schoolId, itemId)
                .stream()
                .map(txn -> InventoryTransactionDto.from(txn, names.get(txn.getAssignedToUserId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryAssigneeDto> listAssignees() {
        return userRepository.findBySchoolIdOrderByCreatedAtDesc(SecurityUtils.currentSchoolId()).stream()
                .map(user -> new InventoryAssigneeDto(user.getId(), user.getDisplayName(), user.getUsername()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long lowStockCount() {
        return itemRepository.countLowStock(SecurityUtils.currentSchoolId());
    }

    @Transactional
    public ImportResult importCsv(MultipartFile file) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        List<String> errors = new ArrayList<>();
        int total = 0;
        int succeeded = 0;
        Map<String, InventoryCategory> categories = new HashMap<>();
        for (InventoryCategory category : categoryRepository.findBySchoolIdOrderByNameAsc(schoolId)) {
            categories.put(category.getName().toLowerCase(Locale.ROOT), category);
        }
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                .withSkipLines(1).build()) {
            String[] row;
            try {
                while ((row = reader.readNext()) != null) {
                    if (row.length == 0 || row[0] == null || row[0].isBlank()) {
                        continue;
                    }
                    total++;
                    try {
                        importRow(schoolId, row, categories);
                        succeeded++;
                    } catch (Exception e) {
                        errors.add("Row " + total + ": " + e.getMessage());
                    }
                }
            } catch (CsvValidationException e) {
                throw new BusinessException("inventory.import_failed");
            }
        } catch (IOException e) {
            throw new BusinessException("inventory.import_failed");
        }
        return new ImportResult(total, succeeded, errors);
    }

    @Transactional(readOnly = true)
    public byte[] exportReport(UUID categoryId, String location, String condition, Boolean lowStock) {
        List<InventoryItemDto> items = listItems(categoryId, location, condition, lowStock);
        return inventoryPdfService.render(SecurityUtils.currentSchoolId(), items);
    }

    public HttpHeaders reportHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"inventory-report.pdf\"");
        return headers;
    }

    private void importRow(UUID schoolId, String[] row, Map<String, InventoryCategory> categories) {
        String name = cell(row, 0);
        String categoryName = cell(row, 1);
        if (name == null || categoryName == null) {
            throw new IllegalArgumentException("name and category are required");
        }
        InventoryCategory category = categories.get(categoryName.toLowerCase(Locale.ROOT));
        if (category == null) {
            throw new IllegalArgumentException("unknown category '" + categoryName + "'");
        }
        String sku = normalizeSku(cell(row, 2));
        if (sku != null && itemRepository.existsBySchoolIdAndSku(schoolId, sku)) {
            throw new IllegalArgumentException("SKU already exists");
        }
        InventoryItem item = new InventoryItem();
        item.setSchoolId(schoolId);
        item.setCategoryId(category.getId());
        item.setName(name);
        item.setSku(sku);
        item.setUnit(cell(row, 3) == null ? "PCS" : cell(row, 3));
        item.setQuantity(parseInt(cell(row, 4), 0));
        item.setReorderThreshold(parseInt(cell(row, 5), 0));
        item.setPurchaseDate(parseDate(cell(row, 6)));
        item.setPurchaseCost(parseDecimal(cell(row, 7)));
        item.setVendor(cell(row, 8));
        item.setLocation(cell(row, 9));
        item.setCondition(parseCondition(cell(row, 10)) == null ? InventoryCondition.GOOD : parseCondition(cell(row, 10)));
        item.setCreatedBy(SecurityUtils.currentUserId());
        InventoryItem saved = itemRepository.save(item);
        if (saved.getQuantity() > 0) {
            recordTxn(saved, InventoryTransactionType.IN, saved.getQuantity(), "CSV import",
                    null, saved.getLocation(), saved.getPurchaseDate() == null ? LocalDate.now() : saved.getPurchaseDate());
        }
    }

    private void applyItemFields(InventoryItem item, InventoryItemRequest request, UUID categoryId, String sku) {
        item.setCategoryId(categoryId);
        item.setName(request.name().trim());
        item.setSku(sku);
        item.setUnit(request.unit() == null || request.unit().isBlank() ? "PCS" : request.unit().trim());
        if (item.getId() == null) {
            item.setQuantity(request.quantity() == null ? 0 : request.quantity());
        }
        item.setReorderThreshold(request.reorderThreshold() == null ? 0 : request.reorderThreshold());
        item.setPurchaseDate(request.purchaseDate());
        item.setPurchaseCost(request.purchaseCost());
        item.setVendor(blankToNull(request.vendor()));
        item.setLocation(blankToNull(request.location()));
        item.setCondition(request.condition() == null ? InventoryCondition.GOOD : request.condition());
    }

    private void recordTxn(InventoryItem item, InventoryTransactionType type, int quantity, String reason,
                           UUID assignedTo, String room, LocalDate date) {
        InventoryTransaction txn = new InventoryTransaction();
        txn.setSchoolId(item.getSchoolId());
        txn.setItemId(item.getId());
        txn.setType(type);
        txn.setQuantity(quantity);
        txn.setReason(reason);
        txn.setAssignedToUserId(assignedTo);
        txn.setRoom(room);
        txn.setTransactionDate(date == null ? LocalDate.now() : date);
        txn.setCreatedBy(SecurityUtils.currentUserId());
        transactionRepository.save(txn);
    }

    private InventoryCategory requireCategory(UUID categoryId, UUID schoolId) {
        return categoryRepository.findByIdAndSchoolId(categoryId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("inventory_category", categoryId));
    }

    private Map<UUID, InventoryCategory> categoriesById(UUID schoolId) {
        Map<UUID, InventoryCategory> map = new HashMap<>();
        for (InventoryCategory category : categoryRepository.findBySchoolIdOrderByNameAsc(schoolId)) {
            map.put(category.getId(), category);
        }
        return map;
    }

    private Map<UUID, String> userNames(UUID schoolId) {
        Map<UUID, String> map = new HashMap<>();
        for (User user : userRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId)) {
            map.put(user.getId(), user.getDisplayName());
        }
        return map;
    }

    private static InventoryItemDto toItemDto(InventoryItem item, InventoryCategory category, Map<UUID, String> names) {
        return InventoryItemDto.from(
                item,
                category == null ? null : category.getName(),
                item.getAssignedToUserId() == null ? null : names.get(item.getAssignedToUserId()));
    }

    private static String normalizeSku(String sku) {
        if (sku == null || sku.isBlank()) {
            return null;
        }
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private static InventoryCondition parseCondition(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return InventoryCondition.valueOf(raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String cell(String[] row, int index) {
        if (row.length <= index || row[index] == null) {
            return null;
        }
        String value = row[index].trim();
        return value.isBlank() ? null : value;
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        return Integer.parseInt(raw);
    }

    private static BigDecimal parseDecimal(String raw) {
        if (raw == null) {
            return null;
        }
        return new BigDecimal(raw);
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null) {
            return null;
        }
        return LocalDate.parse(raw);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
