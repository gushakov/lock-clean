package com.github.lockclean.core.usecase.subscribestudent;

import com.github.lockclean.core.model.course.Course;
import com.github.lockclean.core.model.course.CourseId;
import com.github.lockclean.core.model.student.Student;
import com.github.lockclean.core.model.student.StudentId;
import com.github.lockclean.core.port.ErrorHandlingPresenterOutputPort;

public interface SubscribeStudentPresenterOutputPort extends ErrorHandlingPresenterOutputPort {

    void presentWarningIfSubscriptionExistsAlready(StudentId studentId, CourseId courseId);

    void presentWarningIfCourseCapacityIsExceeded(Course course);

    void presentWarningIfStudentSubscriptionsLimitIsExceeded(Student student);

    /**
     * The subscription was refused because the course or the student was modified by someone
     * else between the moment the business rules were checked and the moment the subscription
     * was about to be written. An expected outcome under concurrency, not an error — the caller
     * may retry.
     */
    void presentWarningIfConcurrentModificationWasDetected(Course course, Student student);

    void presentSuccessfulResultOfSubscribingStudentToCourse(Student student, Course course);
}
