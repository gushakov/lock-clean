package com.github.lockclean.infrastructure.adapter.db;

import com.github.lockclean.core.model.InvalidDomainObjectError;
import com.github.lockclean.core.model.course.Course;
import com.github.lockclean.core.model.course.CourseId;
import com.github.lockclean.core.model.student.Student;
import com.github.lockclean.core.model.student.StudentId;
import com.github.lockclean.core.model.subscription.Subscription;
import com.github.lockclean.core.port.concurrency.OptimisticLockingError;
import com.github.lockclean.core.port.db.PersistenceOperationError;
import com.github.lockclean.core.port.db.PersistenceOperationsOutputPort;
import com.github.lockclean.infrastructure.adapter.db.course.CourseDbEntityRepository;
import com.github.lockclean.infrastructure.adapter.db.map.DbEntityMapper;
import com.github.lockclean.infrastructure.adapter.db.student.StudentDbEntityRepository;
import com.github.lockclean.infrastructure.adapter.db.subscription.SubscriptionDbEntityRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class PersistenceGateway implements PersistenceOperationsOutputPort {

    CourseDbEntityRepository courseRepo;
    StudentDbEntityRepository studentRepo;
    SubscriptionDbEntityRepository subscriptionRepo;
    DbEntityMapper dbEntityMapper;

    /*
        POINT OF INTEREST
        -----------------
        There is no "@Transactional" on this gateway. Transaction demarcation belongs
        to the use case, which draws the boundary through TransactionOperationsOutputPort
        around the writes that must commit together. Each repository call below still
        runs in a transaction of its own (Spring Data's repository implementation is
        itself transactional) or joins the one the use case opened — that is the floor,
        not the boundary.

        Every Spring exception is translated into a port type before it leaves this class:
        a stale-version write becomes OptimisticLockingError (caught first, ahead of the
        broad data-access catch, or it would be swallowed as a plain persistence fault);
        any other data-access failure becomes PersistenceOperationError. The use case
        never sees a framework exception.
     */

    @Override
    public boolean subscriptionExistsAlready(StudentId studentId, CourseId courseId) {
        try {
            return subscriptionRepo.existsByStudentIdAndCourseId(dbEntityMapper.convertStudentIdToString(studentId),
                    dbEntityMapper.convertCourseIdToString(courseId));
        } catch (DataAccessException e) {
            throw new PersistenceOperationError("Cannot check for an existing subscription of student %s to course %s"
                    .formatted(studentId.asString(), courseId.asString()), e);
        }
    }

    @Override
    public int countNumberOfSubscribersToCourse(CourseId courseId) {
        try {
            return subscriptionRepo.countByCourseId(dbEntityMapper.convertCourseIdToString(courseId));
        } catch (DataAccessException e) {
            throw new PersistenceOperationError("Cannot count subscribers to course %s"
                    .formatted(courseId.asString()), e);
        }
    }

    @Override
    public Course obtainCourseById(CourseId courseId) {
        String courseIdStr = dbEntityMapper.convertCourseIdToString(courseId);
        Optional<Course> course;
        try {
            course = courseRepo.findById(courseIdStr).map(dbEntityMapper::map);
        } catch (DataAccessException | InvalidDomainObjectError e) {
            // a corrupt stored row failing reconstitution is an integrity fault of this port
            throw new PersistenceOperationError("Cannot load course with ID %s".formatted(courseIdStr), e);
        }
        return course.orElseThrow(() -> new PersistenceOperationError("Cannot find course with ID %s in the database"
                .formatted(courseIdStr)));
    }

    @Override
    public int countNumberOfCoursesSubscribedForByStudent(StudentId studentId) {
        try {
            return subscriptionRepo.countByStudentId(dbEntityMapper.convertStudentIdToString(studentId));
        } catch (DataAccessException e) {
            throw new PersistenceOperationError("Cannot count courses subscribed for by student %s"
                    .formatted(studentId.asString()), e);
        }
    }

    @Override
    public Student obtainStudentById(StudentId studentId) {
        String studentIdStr = dbEntityMapper.convertStudentIdToString(studentId);
        Optional<Student> student;
        try {
            student = studentRepo.findById(studentIdStr).map(dbEntityMapper::map);
        } catch (DataAccessException | InvalidDomainObjectError e) {
            throw new PersistenceOperationError("Cannot load student with ID %s".formatted(studentIdStr), e);
        }
        return student.orElseThrow(() -> new PersistenceOperationError("Cannot find student with ID %s in the database"
                .formatted(studentIdStr)));
    }

    @Override
    public void saveCourse(Course course) {
        try {
            courseRepo.save(dbEntityMapper.map(course));
        } catch (OptimisticLockingFailureException e) {
            throw new OptimisticLockingError("Course %s was modified concurrently (stale version)"
                    .formatted(course.getId().asString()), e);
        } catch (DataAccessException e) {
            throw new PersistenceOperationError("Cannot save course %s".formatted(course.getId().asString()), e);
        }
    }

    @Override
    public void saveStudent(Student student) {
        try {
            studentRepo.save(dbEntityMapper.map(student));
        } catch (OptimisticLockingFailureException e) {
            throw new OptimisticLockingError("Student %s was modified concurrently (stale version)"
                    .formatted(student.getId().asString()), e);
        } catch (DataAccessException e) {
            throw new PersistenceOperationError("Cannot save student %s".formatted(student.getId().asString()), e);
        }
    }

    @Override
    public void saveSubscription(Subscription subscription) {
        try {
            subscriptionRepo.save(dbEntityMapper.map(subscription));
        } catch (OptimisticLockingFailureException e) {
            throw new OptimisticLockingError("Subscription %s was modified concurrently (stale version)"
                    .formatted(subscription.getId().asString()), e);
        } catch (DataAccessException e) {
            throw new PersistenceOperationError("Cannot save subscription %s"
                    .formatted(subscription.getId().asString()), e);
        }
    }
}
