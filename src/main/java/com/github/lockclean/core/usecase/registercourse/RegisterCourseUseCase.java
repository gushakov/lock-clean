package com.github.lockclean.core.usecase.registercourse;

import com.github.lockclean.core.model.course.Course;
import com.github.lockclean.core.port.db.PersistenceOperationsOutputPort;
import com.github.lockclean.core.port.id.IdsOperationsOutputPort;
import com.github.lockclean.core.port.transaction.TransactionOperationsOutputPort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class RegisterCourseUseCase implements RegisterCourseInputPort {
    RegisterCoursePresenterOutputPort presenter;
    TransactionOperationsOutputPort txOps;
    PersistenceOperationsOutputPort persistenceOps;
    IdsOperationsOutputPort idsOps;

    @Override
    public void registerCourse(String title, int capacity) {
        try {

            // create new instance of a course aggregate
            Course course = Course.builder()
                    .id(idsOps.generateNewCourseId())
                    .title(title)
                    .capacity(capacity)
                    .build();

            // save it in a read-write transaction; a fresh aggregate has no version to
            // lose a race on, so any error here is a genuine fault and propagates
            txOps.doInTransaction(() -> {
                persistenceOps.saveCourse(course);
                txOps.doAfterCommit(() -> presenter.presentSuccessfulResultOfRegisteringNewCourse(course));
            });

        } catch (Exception e) {
            presenter.presentError(e);
        }
    }
}
