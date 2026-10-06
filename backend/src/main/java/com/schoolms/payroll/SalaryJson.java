package com.schoolms.payroll;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.payroll.dto.SalaryLineItemDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class SalaryJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<SalaryLineItemDto>> TYPE = new TypeReference<>() {
    };

    private SalaryJson() {
    }

    public static String toJson(List<SalaryLineItemDto> items) {
        try {
            return MAPPER.writeValueAsString(items == null ? List.of() : items);
        } catch (Exception e) {
            throw new BusinessException("payroll.snapshot_invalid");
        }
    }

    public static List<SalaryLineItemDto> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<SalaryLineItemDto> items = MAPPER.readValue(json, TYPE);
            return items == null ? List.of() : items;
        } catch (Exception e) {
            throw new BusinessException("payroll.snapshot_invalid");
        }
    }

    public static BigDecimal sum(List<SalaryLineItemDto> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return items.stream()
                .map(item -> item.amount() == null ? BigDecimal.ZERO : item.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
