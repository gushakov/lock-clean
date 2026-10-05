package com.github.lockclean.infrastructure;

import com.github.lockclean.core.model.course.Course;
import com.github.lockclean.core.model.course.CourseId;
import com.github.lockclean.core.model.student.Student;
import com.github.lockclean.core.model.student.StudentId;
import com.github.lockclean.core.model.subscription.Subscription;
import com.github.lockclean.core.port.db.PersistenceOperationsOutputPort;
import com.github.lockclean.core.port.id.IdsOperationsOutputPort;
import com.github.lockclean.core.port.transaction.TransactionOperationsOutputPort;
import com.github.lockclean.core.usecase.registercourse.RegisterCourseUseCase;
import com.github.lockclean.core.usecase.registerstudent.RegisterStudentUseCase;
import com.github.lockclean.core.usecase.subscribestudent.SubscribeStudentUseCase;
import com.github.lockclean.infrastructure.adapter.db.course.CourseDbEntityRepository;
import com.github.lockclean.infrastructure.adapter.db.student.StudentDbEntityRepository;
import com.github.lockclean.infrastructure.adapter.db.subscription.SubscriptionDbEntityRepository;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
    POINT OF INTEREST
    -----------------
    This integration test runs against the Dockerized Postgres (docker-compose.yaml) with
    the real adapters, and deliberately WITHOUT a test-managed transaction: with
    "@SpringBootTest" (not a "@DataJdbcTest" slice) the use case's own doInTransaction is
    the outermost transaction, so its commit, its rollback, and the moment the presenter
    is called are the real ones. The rows it creates carry fresh generated ids and are
    left in the database on purpose — they are the experiment's visible trace.
 */

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@FieldDefaults(level = AccessLevel.PRIVATE)
class SubscribeStudentIT {

    @Autowired
    TransactionOperationsOutputPort txOps;

    @Autowired
    PersistenceOperationsOutputPort persistenceOps;

    @Autowired
    IdsOperationsOutputPort idsOps;

    @Autowired
    CourseDbEntityRepository courseRepo;

    @Autowired
    StudentDbEntityRepository studentRepo;

    @Autowired
    SubscriptionDbEntityRepository subscriptionRepo;

    @Test
    void subscribesAndBumpsTheVersionsOfTheAggregatesTheDecisionRestedOn() {
        Course course = registerCourse(20);
        Student student = registerStudent();
        int courseVersion = versionOf(course.getId());
        int studentVersion = versionOf(student.getId());

        RecordingSubscribeStudentPresenter presenter = new RecordingSubscribeStudentPresenter();
        new SubscribeStudentUseCase(presenter, txOps, persistenceOps, idsOps)
                .subscribeStudentToCourse(student.getId().asString(), course.getId().asString(), false);

        // exactly one presentation: the success, after the commit
        assertThat(presenter.successes).hasSize(1);
        assertThat(presenter.warnings).isEmpty();
        assertThat(presenter.errors).isEmpty();

        // the subscription is there, and the unchanged course and student moved one version each:
        // that bump is the lock the next concurrent subscriber will be checked against
        assertThat(subscriptionRepo.existsByStudentIdAndCourseId(student.getId().asString(), course.getId().asString())).isTrue();
        assertThat(versionOf(course.getId())).isEqualTo(courseVersion + 1);
        assertThat(versionOf(student.getId())).isEqualTo(studentVersion + 1);
    }

    @Test
    void losesTheRaceToARivalWriteOnTheCourseAndPresentsExactlyOneWarning() {
        Course course = registerCourse(20);
        Student student = registerStudent();
        int courseVersion = versionOf(course.getId());
        int studentVersion = versionOf(student.getId());

        /*
            POINT OF INTEREST
            -----------------
            The rival write is made deterministic: the persistence port is decorated so that
            on the use case's LAST read (the student), after the course has already been read
            and the capacity rule checked, another "thread" re-saves the course row and bumps
            its version. The course the use case holds is now stale, exactly as in the
            interleaving the article walks through — without threads or a 30-second pause.
         */
        PersistenceOperationsOutputPort contended = new RivalWriteBeforeLastRead(persistenceOps,
                () -> courseRepo.findById(course.getId().asString()).ifPresent(courseRepo::save));

        RecordingSubscribeStudentPresenter presenter = new RecordingSubscribeStudentPresenter();
        new SubscribeStudentUseCase(presenter, txOps, contended, idsOps)
                .subscribeStudentToCourse(student.getId().asString(), course.getId().asString(), false);

        // exactly one presentation: the concurrent-modification warning — no success, no presentError
        assertThat(presenter.warnings).hasSize(1);
        assertThat(presenter.successes).isEmpty();
        assertThat(presenter.errors).isEmpty();

        // the transaction rolled back: no subscription; the course carries the rival's version only;
        // the student was never written (the lambda short-circuited at the losing course save)
        assertThat(subscriptionRepo.existsByStudentIdAndCourseId(student.getId().asString(), course.getId().asString())).isFalse();
        assertThat(versionOf(course.getId())).isEqualTo(courseVersion + 1);
        assertThat(versionOf(student.getId())).isEqualTo(studentVersion);
    }

    // --- helpers: the use cases are assembled here, with the context's adapters ---

    private Course registerCourse(int capacity) {
        RecordingRegisterCoursePresenter presenter = new RecordingRegisterCoursePresenter();
        new RegisterCourseUseCase(presenter, txOps, persistenceOps, idsOps).registerCourse("Latin 102", capacity);
        assertThat(presenter.registered).as("course registered").isNotNull();
        return presenter.registered;
    }

    private Student registerStudent() {
        RecordingRegisterStudentPresenter presenter = new RecordingRegisterStudentPresenter();
        new RegisterStudentUseCase(presenter, txOps, persistenceOps, idsOps).registerStudent("George Clooney");
        assertThat(presenter.registered).as("student registered").isNotNull();
        return presenter.registered;
    }

    private int versionOf(CourseId courseId) {
        return courseRepo.findById(courseId.asString()).orElseThrow().getVersion();
    }

    private int versionOf(StudentId studentId) {
        return studentRepo.findById(studentId.asString()).orElseThrow().getVersion();
    }

    // --- presenters: log like the article's, and record what was presented ---

    static class RecordingRegisterCoursePresenter extends RegisterCourseLoggingPresenter {
        Course registered;

        @Override
        public void presentSuccessfulResultOfRegisteringNewCourse(Course course) {
            super.presentSuccessfulResultOfRegisteringNewCourse(course);
            registered = course;
        }
    }

    static class RecordingRegisterStudentPresenter extends RegisterStudentLoggingPresenter {
        Student registered;

        @Override
        public void presentSuccessfulResultOfRegisteringNewStudent(Student student) {
            super.presentSuccessfulResultOfRegisteringNewStudent(student);
            registered = student;
        }
    }

    static class RecordingSubscribeStudentPresenter extends SubscribeStudentLoggingPresenter {
        final List<String> successes = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        final List<Exception> errors = new ArrayList<>();

        @Override
        public void presentSuccessfulResultOfSubscribingStudentToCourse(Student student, Course course) {
            super.presentSuccessfulResultOfSubscribingStudentToCourse(student, course);
            successes.add(course.getId().asString());
        }

        @Override
        public void presentWarningIfConcurrentModificationWasDetected(Course course, Student student) {
            super.presentWarningIfConcurrentModificationWasDetected(course, student);
            warnings.add(course.getId().asString());
        }

        @Override
        public void presentWarningIfSubscriptionExistsAlready(StudentId studentId, CourseId courseId) {
            super.presentWarningIfSubscriptionExistsAlready(studentId, courseId);
            warnings.add(courseId.asString());
        }

        @Override
        public void presentWarningIfCourseCapacityIsExceeded(Course course) {
            super.presentWarningIfCourseCapacityIsExceeded(course);
            warnings.add(course.getId().asString());
        }

        @Override
        public void presentWarningIfStudentSubscriptionsLimitIsExceeded(Student student) {
            super.presentWarningIfStudentSubscriptionsLimitIsExceeded(student);
            warnings.add(student.getId().asString());
        }

        @Override
        public void presentError(Exception e) {
            super.presentError(e);
            errors.add(e);
        }
    }

    /**
     * Decorates the real persistence port: every call is delegated, and the supplied rival write runs
     * just before the last read of the use case ({@code obtainStudentById}) — i.e. after the course was
     * read and the rules were checked, and before the writes.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    static class RivalWriteBeforeLastRead implements PersistenceOperationsOutputPort {

        PersistenceOperationsOutputPort delegate;
        Runnable rivalWrite;

        RivalWriteBeforeLastRead(PersistenceOperationsOutputPort delegate, Runnable rivalWrite) {
            this.delegate = delegate;
            this.rivalWrite = rivalWrite;
        }

        @Override
        public Student obtainStudentById(StudentId studentId) {
            rivalWrite.run();
            return delegate.obtainStudentById(studentId);
        }

        @Override
        public boolean subscriptionExistsAlready(StudentId studentId, CourseId courseId) {
            return delegate.subscriptionExistsAlready(studentId, courseId);
        }

        @Override
        public int countNumberOfSubscribersToCourse(CourseId courseId) {
            return delegate.countNumberOfSubscribersToCourse(courseId);
        }

        @Override
        public Course obtainCourseById(CourseId courseId) {
            return delegate.obtainCourseById(courseId);
        }

        @Override
        public int countNumberOfCoursesSubscribedForByStudent(StudentId studentId) {
            return delegate.countNumberOfCoursesSubscribedForByStudent(studentId);
        }

        @Override
        public void saveCourse(Course course) {
            delegate.saveCourse(course);
        }

        @Override
        public void saveStudent(Student student) {
            delegate.saveStudent(student);
        }

        @Override
        public void saveSubscription(Subscription subscription) {
            delegate.saveSubscription(subscription);
        }
    }
}
