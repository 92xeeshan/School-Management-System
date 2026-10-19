package com.schoolms.syllabus.dto;

public record SyllabusMaterialFile(
        String fileName,
        String contentType,
        byte[] content
) {
}
