package com.github.lockclean.core.model.course;

import com.github.lockclean.core.model.Validator;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Course {

    @EqualsAndHashCode.Include
    CourseId id;

    String title;

    Integer capacity;

    Integer version;

    @Builder
    public Course(CourseId id, String title, Integer capacity, Integer version) {
        this.id = Validator.notNull(id);
        this.title = Validator.notNullOrBlank(title);
        this.capacity = Validator.strictlyPositive(capacity);
        this.version = version;
    }
}
