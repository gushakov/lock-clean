package com.github.lockclean.infrastructure;

import com.github.lockclean.core.model.course.Course;
import com.github.lockclean.core.model.student.Student;
import com.github.lockclean.core.model.subscription.Subscription;
import com.github.lockclean.core.usecase.subscribestudent.SubscribeStudentInputPort;
import com.github.lockclean.core.usecase.subscribestudent.SubscribeStudentPresenterOutputPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Presenter for testing "subscribe student" use case. Will output messages to console
 * during presentation.
 */
public class SubscribeStudentPresenter implements SubscribeStudentPresenterOutputPort {
    private static final Logger log = LoggerFactory.getLogger(SubscribeStudentPresenter.class);

    @Override
    public void presentErrorOnConcurrentAccessToSubscriptionRelatedEntities(Course course, Student student, Subscription subscription) {
        log.error("Concurrent access was detected during subscription of student with ID {} to course with ID {}", student.getId().asString(),
                course.getId().toString());
    }

    @Override
    public void presentWarningIfCourseCapacityIsExceeded(Course course) {
        log.warn("Course with ID {} has reached its current capacity of {} and cannot accept any more subscriptions",
                course.getId().asString(), course.getCapacity());
    }

    @Override
    public void presentWarningIfStudentSubscriptionsLimitIsExceeded(Student student) {
        log.warn("Subscriptions for student with ID {} exceed current limit of {}", student.getId().asString(),
                SubscribeStudentInputPort.STUDENT_SUBSCRIPTIONS_LIMIT);
    }

    @Override
    public void presentSuccessfulResultOfSubscribingStudentToCourse(Student student, Course course) {
        log.info("Successfully subscribed student with ID {} to course with ID {}", student.getId().asString(),
                course.getId().toString());
    }

    @Override
    public void presentError(Exception e) {
        log.error("Some unknown error has occurred", e);
    }
}
