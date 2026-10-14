package com.schoolms.inventory;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.InventoryCondition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "inventory_item")
@Getter
@Setter
public class InventoryItem extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private String name;

    private String sku;

    @Column(nullable = false)
    private String unit = "PCS";

    @Column(nullable = false)
    private int quantity;

    @Column(name = "reorder_threshold", nullable = false)
    private int reorderThreshold;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_cost")
    private BigDecimal purchaseCost;

    private String vendor;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryCondition condition = InventoryCondition.GOOD;

    @Column(name = "assigned_to_user_id")
    private UUID assignedToUserId;

    @Column(name = "assigned_room")
    private String assignedRoom;

    @Column(name = "created_by")
    private UUID createdBy;

    public boolean isLowStock() {
        return quantity <= reorderThreshold && condition != InventoryCondition.RETIRED;
    }
}
