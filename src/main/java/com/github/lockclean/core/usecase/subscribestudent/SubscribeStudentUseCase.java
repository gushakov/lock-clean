package com.github.lockclean.core.usecase.subscribestudent;

import com.github.lockclean.core.model.course.Course;
import com.github.lockclean.core.model.course.CourseId;
import com.github.lockclean.core.model.student.Student;
import com.github.lockclean.core.model.student.StudentId;
import com.github.lockclean.core.model.subscription.Subscription;
import com.github.lockclean.core.port.db.PersistenceOperationsOutputPort;
import com.github.lockclean.core.port.id.IdsOperationsOutputPort;
import com.github.lockclean.core.port.transaction.TransactionOperationsOutputPort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class SubscribeStudentUseCase implements SubscribeStudentInputPort {

    SubscribeStudentPresenterOutputPort presenter;
    TransactionOperationsOutputPort txOps;
    PersistenceOperationsOutputPort persistenceOps;
    IdsOperationsOutputPort idsOps;

    @Override
    public void subscribeStudentToCourse(String studentIdArg, String courseIdArg, boolean optional) {
        try {

            // prepare and validate input IDs
            CourseId courseId = new CourseId(courseIdArg);
            StudentId studentId = new StudentId(studentIdArg);

            /*
                POINT OF INTEREST
                -----------------
                We need to make sure that we do not create a duplicate subscription.
             */
            if (persistenceOps.subscriptionExistsAlready(studentId, courseId)) {
                presenter.presentWarningIfSubscriptionExistsAlready(studentId, courseId);
                return;
            }

            // obtain the course (aggregate) from the database
            Course course = persistenceOps.obtainCourseById(courseId);

            // query persistence store for the number of students currently subscribed for the course
            int numberOfCourseSubscribers = persistenceOps.countNumberOfSubscribersToCourse(courseId);

            /*
                POINT OF INTEREST
                -----------------
                Check our first business rule (invariant): we cannot subscribe a student
                to a course where the current number of subscribers is already at or over
                the course's capacity.
             */

            if (numberOfCourseSubscribers >= course.getCapacity()) {
                presenter.presentWarningIfCourseCapacityIsExceeded(course);
                return;
            }

            // query persistence store for the number of courses the student is currently subscribed for
            int numberOfCoursesForStudent = persistenceOps.countNumberOfCoursesSubscribedForByStudent(studentId);

            // obtain the student (aggregate) from the database
            Student student = persistenceOps.obtainStudentById(studentId);

            /*
                POINT OF INTEREST
                -----------------
                Check our second business rule (invariant): a student may not subscribe to more
                than 10 courses.
             */

            if (numberOfCoursesForStudent >= STUDENT_SUBSCRIPTIONS_LIMIT) {
                presenter.presentWarningIfStudentSubscriptionsLimitIsExceeded(student);
                return;
            }

            // we have passed all the rules and can create a new subscription aggregate instance
            Subscription subscription = Subscription.builder()
                    .id(idsOps.generateNewSubscriptionId())
                    .courseId(courseId)
                    .studentId(studentId)
                    .optional(optional)
                    .build();

            /*
                POINT OF INTEREST
                -----------------
                Just saving our new subscription here will not exclude a possibility
                of multiple concurrent threads creating subscriptions which do not
                uphold the business rules: a new row carries no version to check.
                We also save the related course and student aggregate instances,
                unchanged, in the same transaction. Their "version" columns are then
                checked and bumped, so a concurrent thread that touched the same
                course or student since we read them loses on one of these writes.

                Any of the three saves may be the losing one. The persistence gateway
                raises OptimisticLockingError, the rest of the lambda is skipped, the
                transaction adapter rolls back and then runs the handler passed as
                the second argument. The handler presents the outcome as a warning;
                this call is the interaction's terminal act, so nothing presents twice.

                We can comment the lines saving course and student to see that the
                race is then no longer detected.
             */

            // one read-write transaction for saving all related aggregates
            txOps.doInTransaction(() -> {

                // save course and student (unchanged) — their versions are the lock
                persistenceOps.saveCourse(course);
                persistenceOps.saveStudent(student);

                // save the new subscription
                persistenceOps.saveSubscription(subscription);

                // this will be executed only once the subscription has been committed
                txOps.doAfterCommit(() -> presenter.presentSuccessfulResultOfSubscribingStudentToCourse(student, course));

            }, () -> presenter.presentWarningIfConcurrentModificationWasDetected(course, student));

        } catch (Exception e) {
            presenter.presentError(e);
        }
    }
}
