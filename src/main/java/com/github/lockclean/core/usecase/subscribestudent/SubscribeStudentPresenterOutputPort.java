package com.github.lockclean.core.usecase.subscribestudent;

import com.github.lockclean.core.model.course.Course;
import com.github.lockclean.core.model.course.CourseId;
import com.github.lockclean.core.model.student.Student;
import com.github.lockclean.core.model.student.StudentId;
import com.github.lockclean.core.model.subscription.Subscription;
import com.github.lockclean.core.port.ErrorHandlingPresenterOutputPort;

public interface SubscribeStudentPresenterOutputPort extends ErrorHandlingPresenterOutputPort {
    void presentErrorOnConcurrentAccessToSubscriptionRelatedEntities(Course course, Student student, Subscription subscription);

    void presentWarningIfCourseCapacityIsExceeded(Course course);

    void presentWarningIfStudentSubscriptionsLimitIsExceeded(Student student);

    void presentSuccessfulResultOfSubscribingStudentToCourse(Student student, Course course);

    void presentWarningIfSubscriptionExistsAlready(StudentId studentId, CourseId courseId);
}
