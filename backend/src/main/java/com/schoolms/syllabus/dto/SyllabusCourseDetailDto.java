package com.schoolms.syllabus.dto;

import java.util.List;

public record SyllabusCourseDetailDto(
        SyllabusCourseDto course,
        List<SyllabusUnitDto> units,
        List<SyllabusMaterialDto> materials
) {
}
