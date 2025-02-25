package com.github.lockclean.core.model.subscription;

import com.github.lockclean.core.model.Validator;
import com.github.lockclean.core.model.course.CourseId;
import com.github.lockclean.core.model.student.StudentId;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Optional;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Subscription {

    @EqualsAndHashCode.Include
    SubscriptionId id;

    StudentId studentId;

    CourseId courseId;

    Boolean optional;

    Integer version;

    @Builder
    public Subscription(SubscriptionId id, StudentId studentId, CourseId courseId, Boolean optional, Integer version) {
        this.id = Validator.notNull(id);
        this.studentId = Validator.notNull(studentId);
        this.courseId = Validator.notNull(courseId);
        this.optional = Optional.ofNullable(optional).orElse(false);
        this.version = version;
    }
}
