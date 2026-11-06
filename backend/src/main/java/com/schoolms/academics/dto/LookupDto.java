package com.schoolms.academics.dto;

import com.schoolms.academics.SchoolLookup;
import com.schoolms.common.enums.LookupKind;

import java.util.UUID;

public record LookupDto(
        UUID id,
        LookupKind kind,
        String name,
        String code,
        Integer sortOrder,
        String status
) {
    public static LookupDto from(SchoolLookup lookup) {
        return new LookupDto(lookup.getId(), lookup.getKind(), lookup.getName(),
                lookup.getCode(), lookup.getSortOrder(), lookup.getStatus());
    }
}
