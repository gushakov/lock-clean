package com.github.lockclean.infrastructure.adapter.db.subscription;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table("subscription")
@Builder
public class SubscriptionDbEntity {

    @Id
    @Column("id")
    String id;

    @Column("student_id")
    String studentId;

    @Column("course_id")
    String courseId;

    @Column("optional")
    Boolean optional;

    @Version
    Integer version;


}
