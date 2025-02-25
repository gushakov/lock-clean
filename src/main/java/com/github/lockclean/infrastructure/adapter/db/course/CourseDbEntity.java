package com.github.lockclean.infrastructure.adapter.db.course;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table("course")
@Builder
public class CourseDbEntity {

    @Id
    @Column("id")
    String id;

    @Column("title")
    String title;

    @Version
    Integer version;
}
