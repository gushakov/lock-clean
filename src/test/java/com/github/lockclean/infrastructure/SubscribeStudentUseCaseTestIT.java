package com.github.lockclean.infrastructure;

import com.github.lockclean.infrastructure.adapter.db.course.CourseDbEntity;
import com.github.lockclean.infrastructure.adapter.db.course.CourseDbEntityRepository;
import com.github.lockclean.infrastructure.adapter.db.student.StudentDbEntityRepository;
import com.github.lockclean.infrastructure.adapter.db.subscription.SubscriptionDbEntityRepository;
import com.github.lockclean.infrastructure.adapter.id.JNanoIdGenerator;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.test.annotation.Rollback;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

/*
    This is an integration test with uses "slice" technique to bootstrap
    only the needed Spring Boot Data JDBC infrastructure plus the components
    scanned from "infrastructure" package and its subpackages.
 */

@Testcontainers
@DataJdbcTest
@Rollback(value = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
class SubscribeStudentUseCaseTestIT {

    // Testcontainers container for Postgres
    @Container
    private static PostgreSQLContainer<?> pgContainer;

    @BeforeAll
    static void beforeAll() {
        // configure and start test container
        pgContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"))
                .withDatabaseName("lockdb")
                .withUsername("postgres")
                .withPassword("postgres")
                .withReuse(false);
        pgContainer.setPortBindings(List.of("32779:5432"));
        pgContainer.start();
    }

    @SpringBootConfiguration
    @EnableJdbcRepositories
    @ComponentScan(basePackageClasses = {SubscribeStudentUseCaseTestIT.class})
    static class TestConfig {
        // configuring "slice" test with Spring Boot
    }

    @Autowired
    CourseDbEntityRepository courseRepo;

    @Autowired
    StudentDbEntityRepository studentRepo;

    @Autowired
    SubscriptionDbEntityRepository subscriptionRepo;

    @Autowired
    JNanoIdGenerator idGenerator;

    @Test
    void test_setup_ok() {

    }

    @Test
    void save_course() {

        String courseId = idGenerator.generateNewCourseId().asString();


        // create a course DB entity
        CourseDbEntity newCourse = CourseDbEntity.builder()
                .id(courseId)
                .title("Latin 101")
                .capacity(20)
                .build();

        // save course
        courseRepo.save(newCourse);

    }

    @AfterAll
    static void afterAll() {
        // uncomment to pause execution so that we can inspect the contents of the database
        try {
//            Thread.sleep(Duration.ofHours(1));
        } catch (Exception e) {
            // ignore
        }
        finally {
            pgContainer.stop();
        }
    }
}
