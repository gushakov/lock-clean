package com.github.lockclean.infrastructure.adapter.db.student;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table("student")
@Builder
public class StudentDbEntity {

    @Id
    @Column("id")
    String id;

    @Column("full_name")
    String fullName;

    @Version
    Integer version;


}
