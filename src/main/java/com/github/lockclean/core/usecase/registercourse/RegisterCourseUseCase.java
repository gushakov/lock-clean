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

            // start read-write transaction
            txOps.doInTransaction(false, () -> {
                // create new instance of a course aggregate and save it
                Course course = Course.builder()
                        .id(idsOps.generateNewCourseId())
                        .title(title)
                        .capacity(capacity)
                        .build();
                persistenceOps.saveCourse(course);
                txOps.doAfterCommit(() -> presenter.presentSuccessfulResultOfRegisteringNewCourse(course));
            });

        } catch (Exception e) {
            presenter.presentError(e);
        }
    }
}
