package com.github.lockclean.core.usecase.registercourse;

import com.github.lockclean.core.model.course.Course;
import com.github.lockclean.core.model.course.CourseId;
import com.github.lockclean.core.port.concurrency.OptimisticLockingError;
import com.github.lockclean.core.port.db.PersistenceOperationsOutputPort;
import com.github.lockclean.core.port.id.IdsOperationsOutputPort;
import com.github.lockclean.core.usecase.InlineTransactionOperations;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Interaction tests for {@link RegisterCourseUseCase}. Registering is a <b>single-writer</b>
 * interaction: a freshly built aggregate has no version to lose a race on, so the use case keeps
 * the propagating default rather than the lock-detect handler. The second test pins that decision —
 * should a lock error ever reach this write, it is a wiring surprise and must arrive at
 * {@code presentError} as itself, with no named outcome and no success presented.
 */
class RegisterCourseUseCaseTest {

    private final RegisterCoursePresenterOutputPort presenter = mock(RegisterCoursePresenterOutputPort.class);
    private final PersistenceOperationsOutputPort persistenceOps = mock(PersistenceOperationsOutputPort.class);
    private final IdsOperationsOutputPort idsOps = mock(IdsOperationsOutputPort.class);

    private final RegisterCourseUseCase useCase =
            new RegisterCourseUseCase(presenter, new InlineTransactionOperations(), persistenceOps, idsOps);

    @Test
    void savesANewCourseAndPresentsItOnceAfterCommit() {
        when(idsOps.generateNewCourseId()).thenReturn(new CourseId("crs_f8oIXk"));

        useCase.registerCourse("Latin 102", 20);

        ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(persistenceOps).saveCourse(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo("Latin 102");
        assertThat(saved.getValue().getCapacity()).isEqualTo(20);
        assertThat(saved.getValue().getVersion()).as("a new aggregate carries no version").isNull();

        verify(presenter).presentSuccessfulResultOfRegisteringNewCourse(saved.getValue());
        verifyNoMoreInteractions(presenter);
    }

    @Test
    void letsAnOptimisticLockErrorReachPresentErrorAsItself() {
        when(idsOps.generateNewCourseId()).thenReturn(new CourseId("crs_f8oIXk"));
        OptimisticLockingError unexpected = new OptimisticLockingError("stale version", null);
        doThrow(unexpected).when(persistenceOps).saveCourse(any());

        useCase.registerCourse("Latin 102", 20);

        verify(presenter).presentError(unexpected);
        verifyNoMoreInteractions(presenter);
    }

    @Test
    void presentsAnInvalidCapacityAsAnErrorBeforeAnyWrite() {
        when(idsOps.generateNewCourseId()).thenReturn(new CourseId("crs_f8oIXk"));

        useCase.registerCourse("Latin 102", 0);

        verify(persistenceOps, never()).saveCourse(any());
        verify(presenter).presentError(any());
        verifyNoMoreInteractions(presenter);
    }
}
