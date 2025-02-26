package com.github.lockclean.core.usecase.subscribestudent;

import com.github.lockclean.core.model.course.Course;
import com.github.lockclean.core.model.course.CourseId;
import com.github.lockclean.core.model.student.Student;
import com.github.lockclean.core.model.student.StudentId;
import com.github.lockclean.core.model.subscription.Subscription;
import com.github.lockclean.core.port.db.ConcurrentPersistenceEntityAccessError;
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
                throw new CourseSubscriptionsCapacityExceededError(courseId, course.getCapacity());
            }

            // query persistence store for the number of courses the student is currently subscribed for
            int numberOfCoursesForStudent = persistenceOps.countNumberOfCoursesSubscribedForByStudent(studentId);

            /*
                POINT OF INTEREST
                -----------------
                Check our second business rule (invariant): a student may not subscribe to more
                than 10 courses.
             */

            if (numberOfCoursesForStudent >= STUDENT_SUBSCRIPTIONS_LIMIT) {
                throw new StudentSubscriptionsLimitExceededError(studentId);
            }

            // obtain the student (aggregate) from the database
            Student student = persistenceOps.obtainStudentById(studentId);

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
                uphold the business rules. We also need to save the related course
                and student aggregate instances as well. Only this will guarantee
                that the optimal concurrency lock on our aggregates will be detected
                in the case of contention for the same aggregate instances by different
                threads running this use case in parallel.
             */

            // start a new (read-write) transaction for saving all related aggregates
            txOps.doInTransaction(false, () -> {

                try {
                    // save course
                    persistenceOps.saveCourse(course);
                    // save student
                    persistenceOps.saveStudent(student);
                    // save subscription
                    persistenceOps.saveSubscription(subscription);
                } catch (ConcurrentPersistenceEntityAccessError e) {
                    // TODO: implement
                }

            });


        } catch (Exception e) {
            presenter.presentError(e);
        }
    }
}
