package com.github.lockclean.core.usecase.subscribestudent;

import com.github.lockclean.core.model.course.Course;
import com.github.lockclean.core.model.course.CourseId;
import com.github.lockclean.core.model.student.Student;
import com.github.lockclean.core.model.student.StudentId;
import com.github.lockclean.core.model.subscription.Subscription;
import com.github.lockclean.core.model.subscription.SubscriptionId;
import com.github.lockclean.core.port.concurrency.OptimisticLockingError;
import com.github.lockclean.core.port.db.PersistenceOperationError;
import com.github.lockclean.core.port.db.PersistenceOperationsOutputPort;
import com.github.lockclean.core.port.id.IdsOperationsOutputPort;
import com.github.lockclean.core.usecase.InlineTransactionOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Interaction tests for {@link SubscribeStudentUseCase} against mocked ports. They pin the
 * presentation discipline around the one transaction that can lose a race: exactly one
 * {@code present*} per run, the concurrent-modification warning on a lost race (never
 * {@code presentError}, never the success), the success only when all three saves went through.
 */
class SubscribeStudentUseCaseTest {

    private static final CourseId COURSE_ID = new CourseId("crs_f8oIXk");
    private static final StudentId STUDENT_ID = new StudentId("stu_KCpVjR");

    private final SubscribeStudentPresenterOutputPort presenter = mock(SubscribeStudentPresenterOutputPort.class);
    private final PersistenceOperationsOutputPort persistenceOps = mock(PersistenceOperationsOutputPort.class);
    private final IdsOperationsOutputPort idsOps = mock(IdsOperationsOutputPort.class);

    private final SubscribeStudentUseCase useCase =
            new SubscribeStudentUseCase(presenter, new InlineTransactionOperations(), persistenceOps, idsOps);

    private final Course course = Course.builder().id(COURSE_ID).title("Latin 102").capacity(20).version(3).build();
    private final Student student = Student.builder().id(STUDENT_ID).fullName("George Clooney").version(1).build();

    @BeforeEach
    void rulesPass() {
        when(persistenceOps.subscriptionExistsAlready(STUDENT_ID, COURSE_ID)).thenReturn(false);
        when(persistenceOps.obtainCourseById(COURSE_ID)).thenReturn(course);
        when(persistenceOps.countNumberOfSubscribersToCourse(COURSE_ID)).thenReturn(5);
        when(persistenceOps.countNumberOfCoursesSubscribedForByStudent(STUDENT_ID)).thenReturn(2);
        when(persistenceOps.obtainStudentById(STUDENT_ID)).thenReturn(student);
        when(idsOps.generateNewSubscriptionId()).thenReturn(new SubscriptionId("sub_Qz7nP2"));
    }

    @Test
    void savesTheUnchangedCourseAndStudentWithTheNewSubscriptionAndPresentsSuccessOnce() {
        useCase.subscribeStudentToCourse(STUDENT_ID.asString(), COURSE_ID.asString(), true);

        // the re-saves carry the very instances (and versions) the decision rested on
        verify(persistenceOps).saveCourse(course);
        verify(persistenceOps).saveStudent(student);
        ArgumentCaptor<Subscription> saved = ArgumentCaptor.forClass(Subscription.class);
        verify(persistenceOps).saveSubscription(saved.capture());
        assertThat(saved.getValue().getCourseId()).isEqualTo(COURSE_ID);
        assertThat(saved.getValue().getStudentId()).isEqualTo(STUDENT_ID);
        assertThat(saved.getValue().getOptional()).isTrue();

        verify(presenter).presentSuccessfulResultOfSubscribingStudentToCourse(student, course);
        verifyNoMoreInteractions(presenter);
    }

    @Test
    void presentsTheConcurrentModificationWarningExactlyOnceWhenTheCourseSaveLosesTheRace() {
        doThrow(new OptimisticLockingError("stale course version", null)).when(persistenceOps).saveCourse(course);

        useCase.subscribeStudentToCourse(STUDENT_ID.asString(), COURSE_ID.asString(), false);

        // the losing save short-circuits the rest of the transaction
        verify(persistenceOps, never()).saveStudent(any());
        verify(persistenceOps, never()).saveSubscription(any());

        // one warning, no success, no presentError
        verify(presenter).presentWarningIfConcurrentModificationWasDetected(course, student);
        verifyNoMoreInteractions(presenter);
    }

    @Test
    void presentsTheConcurrentModificationWarningWhenTheStudentSaveLosesTheRace() {
        doThrow(new OptimisticLockingError("stale student version", null)).when(persistenceOps).saveStudent(student);

        useCase.subscribeStudentToCourse(STUDENT_ID.asString(), COURSE_ID.asString(), false);

        verify(persistenceOps).saveCourse(course);
        verify(persistenceOps, never()).saveSubscription(any());
        verify(presenter).presentWarningIfConcurrentModificationWasDetected(course, student);
        verifyNoMoreInteractions(presenter);
    }

    @Test
    void routesAPersistenceFaultInsideTheTransactionToPresentErrorAsItself() {
        PersistenceOperationError fault = new PersistenceOperationError("database unavailable");
        doThrow(fault).when(persistenceOps).saveSubscription(any());

        useCase.subscribeStudentToCourse(STUDENT_ID.asString(), COURSE_ID.asString(), false);

        // a fault is not a lost race: it rides the outermost checkpoint, not the lock handler
        verify(presenter).presentError(fault);
        verifyNoMoreInteractions(presenter);
    }

    @Test
    void refusesWhenTheCourseIsAtCapacityBeforeAnyWrite() {
        when(persistenceOps.countNumberOfSubscribersToCourse(COURSE_ID)).thenReturn(20);

        useCase.subscribeStudentToCourse(STUDENT_ID.asString(), COURSE_ID.asString(), false);

        verify(persistenceOps, never()).saveCourse(any());
        verify(persistenceOps, never()).saveStudent(any());
        verify(persistenceOps, never()).saveSubscription(any());
        verify(presenter).presentWarningIfCourseCapacityIsExceeded(course);
        verifyNoMoreInteractions(presenter);
    }
}
