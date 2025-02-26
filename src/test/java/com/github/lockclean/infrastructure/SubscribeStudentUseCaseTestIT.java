package com.github.lockclean.infrastructure;

import com.github.lockclean.core.model.course.Course;
import com.github.lockclean.core.model.course.CourseId;
import com.github.lockclean.infrastructure.adapter.db.PersistenceGateway;
import com.github.lockclean.infrastructure.adapter.id.JNanoIdGenerator;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.test.annotation.Rollback;

/*
    This is an integration test with uses "slice" technique to bootstrap
    Spring Boot Data JDBC infrastructure. "ComponentScan" annotation will
    assure that we discover all the components from "infrastructure" package
    and its sub-packages.
 */

@DataJdbcTest
@Rollback(value = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SubscribeStudentUseCaseTestIT {

    @SpringBootConfiguration
    @EnableJdbcRepositories
    @ComponentScan(basePackageClasses = {SubscribeStudentUseCaseTestIT.class})
    static class TestConfig {
        // configuring "slice" test with Spring Boot
    }

    @Autowired
    PersistenceGateway persistenceGateway;

    @Autowired
    JNanoIdGenerator idGenerator;

    @Test
    void save_course() {

        String courseId = idGenerator.generateNewCourseId().asString();

        // create a course
        Course course = Course.builder()
                .id(new CourseId(courseId))
                .title("Latin 101")
                .capacity(20)
                .build();

        // save course
        persistenceGateway.saveCourse(course);

    }

}
