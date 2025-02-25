package com.github.lockclean.infrastructure.adapter.db;

import com.github.lockclean.infrastructure.adapter.db.course.CourseDbEntity;
import com.github.lockclean.infrastructure.adapter.db.course.CourseDbEntityRepository;
import com.github.lockclean.infrastructure.adapter.db.student.StudentDbEntity;
import com.github.lockclean.infrastructure.adapter.db.student.StudentDbEntityRepository;
import com.github.lockclean.infrastructure.adapter.db.subscription.SubscriptionDbEntity;
import com.github.lockclean.infrastructure.adapter.db.subscription.SubscriptionDbEntityRepository;
import com.github.lockclean.infrastructure.adapter.id.JNanoIdGenerator;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.test.annotation.Rollback;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

// run Spring Boot "slice" test autoconfiguring Spring Data JDBC infrastructure beans only
@DataJdbcTest
@Rollback(false) // set to "false", if we want to inspect the results in the database
@FieldDefaults(level = AccessLevel.PRIVATE)
class OptimisticLockTestIT {

    // set up and run a Testcontainer for Postgres database
    @SpringBootConfiguration
    @EnableJdbcRepositories
    @ComponentScan(basePackageClasses = JNanoIdGenerator.class)
    static class TestConfig {
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgresContainer() {

            PostgreSQLContainer<?> pgContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"))
                    .withDatabaseName("lockdb")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withReuse(true);
            // bind to a known port on the local machine (must be available)
            pgContainer.setPortBindings(List.of("32779:5432"));

            return pgContainer;
        }
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
    void create_course_save_it_twice_version_must_be_updated() {

        String courseId = idGenerator.generateNewCourseId().asString();

        // create a course DB entity
        CourseDbEntity newCourse = CourseDbEntity.builder()
                .id(courseId)
                .title("Latin 101")
                .build();

        // save course
        courseRepo.save(newCourse);

        // load the same course
        CourseDbEntity loadedCourse = courseRepo.findById(courseId).orElseThrow();

        // the version has been assigned to a default non-null value: zero
        Assertions.assertThat(loadedCourse.getVersion()).isZero();

        /*
            POINT OF INTEREST
            -----------------
            We are not changing a saved course entity, but upon
            the repeated save, Spring Data JDBC, will check and
            increment the "version" for us anyway.
         */
        courseRepo.save(loadedCourse);
        CourseDbEntity savedAgainCourse = courseRepo.findById(courseId).orElseThrow();
        // the version has been updated
        Assertions.assertThat(savedAgainCourse.getVersion()).isOne();
    }

    @Test
    void count_subscriptions_by_course_id() {

        String courseId = idGenerator.generateNewCourseId().asString();

        CourseDbEntity course = CourseDbEntity.builder()
                .id(courseId)
                .title("Title of %s".formatted(courseId))
                .build();
        courseRepo.save(course);

        String studentId = idGenerator.generateNewStudentId().asString();
        StudentDbEntity student = StudentDbEntity.builder()
                .id(studentId)
                .fullName("Name of %s".formatted(studentId))
                .build();
        studentRepo.save(student);

        String subscriptionId = idGenerator.generateNewSubscriptionId().asString();
        SubscriptionDbEntity subscription = SubscriptionDbEntity.builder()
                .id(subscriptionId)
                .studentId(studentId)
                .courseId(courseId)
                .optional(false)
                .build();
        subscriptionRepo.save(subscription);

        Assertions.assertThat(subscriptionRepo.countByCourseId(courseId)).isEqualTo(1);
        Assertions.assertThat(subscriptionRepo.countByStudentId(studentId)).isEqualTo(1);

    }

    @AfterAll
    static void keepDatabaseRunning() throws InterruptedException {
        // uncomment to be able to inspect the contents of the database
//        Thread.sleep(java.time.Duration.ofHours(1));
    }
}
