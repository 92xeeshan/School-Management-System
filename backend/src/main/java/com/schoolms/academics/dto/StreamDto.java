package com.schoolms.academics.dto;

import com.schoolms.academics.Stream;

import java.util.UUID;

public record StreamDto(
        UUID id,
        String name,
        String code,
        Integer sortOrder,
        String status
) {
    public static StreamDto from(Stream stream) {
        return new StreamDto(stream.getId(), stream.getName(), stream.getCode(),
                stream.getSortOrder(), stream.getStatus());
    }
}
