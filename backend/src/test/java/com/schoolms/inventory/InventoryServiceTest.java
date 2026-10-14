package com.schoolms.inventory;

import com.schoolms.TestSecurity;
import com.schoolms.common.enums.InventoryCondition;
import com.schoolms.common.enums.InventoryTransactionType;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.inventory.dto.InventoryItemRequest;
import com.schoolms.inventory.dto.InventoryTransactionRequest;
import com.schoolms.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private InventoryCategoryRepository categoryRepository;
    @Mock private InventoryItemRepository itemRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private InventoryPdfService inventoryPdfService;

    private InventoryService inventoryService;

    private final UUID schoolId = TestSecurity.SCHOOL_ID;
    private final UUID categoryId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    private final UUID itemId = UUID.fromString("b2000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(
                categoryRepository, itemRepository, transactionRepository, userRepository, inventoryPdfService);
        TestSecurity.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void createItemPersistsForSchoolAndRecordsOpeningStock() {
        when(categoryRepository.findByIdAndSchoolId(categoryId, schoolId)).thenReturn(Optional.of(category()));
        when(itemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> {
            InventoryItem item = inv.getArgument(0);
            if (item.getId() == null) {
                item.setId(itemId);
            }
            return item;
        });
        when(transactionRepository.save(any(InventoryTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId)).thenReturn(List.of());

        var dto = inventoryService.createItem(new InventoryItemRequest(
                categoryId, "Digital projector", "lab-prj-001", "PCS", 2, 1,
                LocalDate.of(2025, 6, 12), new BigDecimal("28500.00"),
                "AV Systems", "Lab 1", InventoryCondition.GOOD));

        ArgumentCaptor<InventoryItem> captor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(itemRepository).save(captor.capture());
        assertEquals(schoolId, captor.getValue().getSchoolId());
        assertEquals("Digital projector", dto.name());
        assertEquals("LAB-PRJ-001", dto.sku());
        assertEquals(2, dto.quantity());
        assertEquals("Lab Equipment", dto.categoryName());
        verify(transactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    void stockOutReducesQuantity() {
        InventoryItem item = item(4);
        when(itemRepository.findByIdAndSchoolId(itemId, schoolId)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(InventoryTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.findByIdAndSchoolId(categoryId, schoolId)).thenReturn(Optional.of(category()));
        when(userRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId)).thenReturn(List.of());

        var dto = inventoryService.recordTransaction(itemId, new InventoryTransactionRequest(
                InventoryTransactionType.OUT, 1, "Issued to office", null, "Admin office", LocalDate.now()));

        assertEquals(3, dto.quantity());
        assertEquals(3, item.getQuantity());
    }

    @Test
    void stockOutNeverGoesNegative() {
        InventoryItem item = item(1);
        when(itemRepository.findByIdAndSchoolId(itemId, schoolId)).thenReturn(Optional.of(item));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                inventoryService.recordTransaction(itemId, new InventoryTransactionRequest(
                        InventoryTransactionType.OUT, 5, "Over-issue", null, null, LocalDate.now())));

        assertEquals("inventory.insufficient_stock", ex.getMessage());
        assertEquals(1, item.getQuantity());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void checkoutAssignsAssetAndCheckinClearsIt() {
        InventoryItem item = item(1);
        UUID teacherId = UUID.fromString("20000000-0000-0000-0000-000000000003");
        when(itemRepository.findByIdAndSchoolId(itemId, schoolId)).thenReturn(Optional.of(item));
        when(userRepository.findByIdAndSchoolId(teacherId, schoolId)).thenReturn(Optional.of(new com.schoolms.user.User()));
        when(itemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(InventoryTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.findByIdAndSchoolId(categoryId, schoolId)).thenReturn(Optional.of(category()));
        when(userRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId)).thenReturn(List.of());

        inventoryService.recordTransaction(itemId, new InventoryTransactionRequest(
                InventoryTransactionType.CHECKOUT, 0, "Class use", teacherId, "Room 12", LocalDate.now()));
        assertEquals(teacherId, item.getAssignedToUserId());
        assertEquals("Room 12", item.getAssignedRoom());

        inventoryService.recordTransaction(itemId, new InventoryTransactionRequest(
                InventoryTransactionType.CHECKIN, 0, "Returned", null, null, LocalDate.now()));
        assertEquals(null, item.getAssignedToUserId());
        assertEquals(null, item.getAssignedRoom());
    }

    @Test
    void lowStockFlagWhenAtOrBelowThreshold() {
        InventoryItem item = item(3);
        item.setReorderThreshold(3);
        assertTrue(item.isLowStock());
        item.setQuantity(4);
        assertTrue(!item.isLowStock());
    }

    private InventoryCategory category() {
        InventoryCategory category = new InventoryCategory();
        category.setId(categoryId);
        category.setSchoolId(schoolId);
        category.setName("Lab Equipment");
        return category;
    }

    private InventoryItem item(int quantity) {
        InventoryItem item = new InventoryItem();
        item.setId(itemId);
        item.setSchoolId(schoolId);
        item.setCategoryId(categoryId);
        item.setName("Digital projector");
        item.setSku("LAB-PRJ-001");
        item.setUnit("PCS");
        item.setQuantity(quantity);
        item.setReorderThreshold(1);
        item.setCondition(InventoryCondition.GOOD);
        return item;
    }
}
