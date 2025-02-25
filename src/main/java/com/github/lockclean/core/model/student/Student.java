package com.github.lockclean.core.model.student;

import com.github.lockclean.core.model.Validator;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Student {

    @EqualsAndHashCode.Include
    StudentId id;

    String fullName;

    Integer version;

    @Builder
    public Student(StudentId id, String fullName, Integer version) {
        this.id = Validator.notNull(id);
        this.fullName = Validator.notNullOrBlank(fullName);
        this.version = version;
    }
}
