package com.schoolms.academics;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "section")
@Getter
@Setter
public class Section extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer capacity = 40;

    @Column(length = 50)
    private String room;
}
