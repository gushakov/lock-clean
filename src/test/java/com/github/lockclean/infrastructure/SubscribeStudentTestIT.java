package com.github.lockclean.infrastructure;

import com.github.lockclean.core.usecase.registercourse.RegisterCourseInputPort;
import com.github.lockclean.core.usecase.registerstudent.RegisterStudentInputPort;
import com.github.lockclean.core.usecase.subscribestudent.SubscribeStudentInputPort;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.test.annotation.Rollback;

/*
    This is an integration test with uses "slice" technique to bootstrap
    only Spring Boot Data JDBC infrastructure. "ComponentScan" annotation
    will assure that we also discover all the components from "infrastructure"
    package and its sub-packages.
 */

@DataJdbcTest
@Rollback(value = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SubscribeStudentTestIT {

    @SpringBootConfiguration
    @EnableJdbcRepositories
    @ComponentScan(basePackageClasses = {SubscribeStudentTestIT.class})
    static class TestConfig {
        // Spring Boot Test local configuration
    }

    @Autowired
    ApplicationContext applicationContext;

    @Test
    void register_new_course() {
        registerCourseUseCase().registerCourse("Latin 102", 20);
    }

    @Test
    void register_new_student() {
        registerStudentUseCase().registerStudent("George Clooney");
    }

    @Test
    void subscribe_student_to_course() {

        /*
            We can see the IDs for courses and students in the database.
         */

        subscribeStudentUseCase().subscribeStudentToCourse("stu_mwAW55", "crs_YhDCjy", false);
    }

    // these methods will get prototype beans for each use case from the application context

    private RegisterCourseInputPort registerCourseUseCase() {
        return applicationContext.getBean(RegisterCourseInputPort.class);
    }

    private RegisterStudentInputPort registerStudentUseCase() {
        return applicationContext.getBean(RegisterStudentInputPort.class);
    }

    private SubscribeStudentInputPort subscribeStudentUseCase() {
        return applicationContext.getBean(SubscribeStudentInputPort.class);
    }

}
