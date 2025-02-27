package com.github.lockclean.infrastructure;

import com.github.lockclean.core.model.course.Course;
import com.github.lockclean.core.usecase.registercourse.RegisterCoursePresenterOutputPort;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegisterCourseLoggingPresenter implements RegisterCoursePresenterOutputPort {
    @Override
    public void presentSuccessfulResultOfRegisteringNewCourse(Course course) {
        log.info("Registered new course with title: {}, capacity: {}, ID: {}", course.getTitle(), course.getCapacity(), course.getId().asString());
    }
}
