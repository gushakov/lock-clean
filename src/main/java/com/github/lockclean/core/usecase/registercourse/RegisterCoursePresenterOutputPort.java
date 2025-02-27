package com.github.lockclean.core.usecase.registercourse;

import com.github.lockclean.core.model.course.Course;
import com.github.lockclean.core.port.ErrorHandlingPresenterOutputPort;

public interface RegisterCoursePresenterOutputPort extends ErrorHandlingPresenterOutputPort {
    void presentSuccessfulResultOfRegisteringNewCourse(Course course);
}
