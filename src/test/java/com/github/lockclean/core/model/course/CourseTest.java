package com.github.lockclean.core.model.course;

import com.github.lockclean.core.model.InvalidDomainObjectError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

public class CourseTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void must_not_construct_course_id_with_null_blank_id(String id) {
        Assertions.assertThrows(InvalidDomainObjectError.class, () -> new CourseId(id));
    }
}
