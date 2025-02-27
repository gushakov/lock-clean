package com.github.lockclean.infrastructure.adapter.db;

import com.github.lockclean.core.model.course.Course;
import com.github.lockclean.core.model.course.CourseId;
import com.github.lockclean.core.model.student.Student;
import com.github.lockclean.core.model.student.StudentId;
import com.github.lockclean.core.model.subscription.Subscription;
import com.github.lockclean.core.port.db.ConcurrentPersistenceEntityAccessError;
import com.github.lockclean.core.port.db.PersistenceOperationError;
import com.github.lockclean.core.port.db.PersistenceOperationsOutputPort;
import com.github.lockclean.infrastructure.adapter.db.course.CourseDbEntityRepository;
import com.github.lockclean.infrastructure.adapter.db.map.DbEntityMapper;
import com.github.lockclean.infrastructure.adapter.db.student.StudentDbEntityRepository;
import com.github.lockclean.infrastructure.adapter.db.subscription.SubscriptionDbEntityRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        We wrap each method of the persistence gateway (adapter) in a transaction
        using Spring's "@Transactional" annotation. The Spring will either create
        a new transaction or propagate the transaction started by a use case.
        Default isolation level is "READ COMMITED": each transaction can read
        commited data.
     */

    @Override
    @Transactional(readOnly = true)
    public boolean subscriptionExistsAlready(StudentId studentId, CourseId courseId) {
        return subscriptionRepo.existsByStudentIdAndCourseId(dbEntityMapper.convertStudentIdToString(studentId),
                dbEntityMapper.convertCourseIdToString(courseId));
    }

    @Override
    @Transactional(readOnly = true)
    public int countNumberOfSubscribersToCourse(CourseId courseId) {
        try {
            return subscriptionRepo.countByCourseId(dbEntityMapper.convertCourseIdToString(courseId));
        } catch (Exception e) {
            throw new PersistenceOperationError(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Course obtainCourseById(CourseId courseId) {
        try {
            String courseIdStr = dbEntityMapper.convertCourseIdToString(courseId);
            return courseRepo.findById(courseIdStr)
                    .map(dbEntityMapper::map)
                    .orElseThrow(() -> new PersistenceOperationError("Cannot find course with ID %s in the database"
                            .formatted(courseIdStr)));
        } catch (Exception e) {
            throw new PersistenceOperationError(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countNumberOfCoursesSubscribedForByStudent(StudentId studentId) {
        try {
            return subscriptionRepo.countByStudentId(dbEntityMapper.convertStudentIdToString(studentId));
        } catch (Exception e) {
            throw new PersistenceOperationError(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Student obtainStudentById(StudentId studentId) {
        String studentIdStr = dbEntityMapper.convertStudentIdToString(studentId);
        try {
            return studentRepo.findById(studentIdStr)
                    .map(dbEntityMapper::map)
                    .orElseThrow(() -> new PersistenceOperationError("Cannot find student with ID %s in the database"
                            .formatted(studentIdStr)));
        } catch (Exception e) {
            throw new PersistenceOperationError(e);
        }
    }

    @Override
    @Transactional
    public void saveCourse(Course course) {
        try {
            courseRepo.save(dbEntityMapper.map(course));
        } catch (OptimisticLockingFailureException e) {
            throw new ConcurrentPersistenceEntityAccessError(e);
        } catch (Exception e) {
            throw new PersistenceOperationError(e);
        }
    }

    @Override
    @Transactional
    public void saveStudent(Student student) {
        try {
            studentRepo.save(dbEntityMapper.map(student));
        } catch (OptimisticLockingFailureException e) {
            throw new ConcurrentPersistenceEntityAccessError(e);
        } catch (Exception e) {
            throw new PersistenceOperationError(e);
        }
    }

    @Override
    @Transactional
    public void saveSubscription(Subscription subscription) {
        try {
            subscriptionRepo.save(dbEntityMapper.map(subscription));
        } catch (OptimisticLockingFailureException e) {
            throw new ConcurrentPersistenceEntityAccessError(e);
        } catch (Exception e) {
            throw new PersistenceOperationError(e);
        }
    }
}
