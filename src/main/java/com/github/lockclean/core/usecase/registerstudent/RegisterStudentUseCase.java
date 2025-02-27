package com.github.lockclean.core.usecase.registerstudent;

import com.github.lockclean.core.model.student.Student;
import com.github.lockclean.core.port.db.PersistenceOperationsOutputPort;
import com.github.lockclean.core.port.id.IdsOperationsOutputPort;
import com.github.lockclean.core.port.transaction.TransactionOperationsOutputPort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class RegisterStudentUseCase implements RegisterStudentInputPort {

    RegisterStudentPresenterOutputPort presenter;
    TransactionOperationsOutputPort txOps;
    PersistenceOperationsOutputPort persistenceOps;
    IdsOperationsOutputPort idsOps;


    @Override
    public void registerStudent(String fullName) {

        try {
            // create new student aggregate instance and save it in a new read-write transaction
            txOps.doInTransaction(false, () -> {
                Student student = Student.builder()
                        .id(idsOps.generateNewStudentId())
                        .fullName(fullName)
                        .build();
                persistenceOps.saveStudent(student);
                txOps.doAfterCommit(() -> presenter.presentSuccessfulResultOfRegisteringNewStudent(student));
            });
        } catch (Exception e) {
            presenter.presentError(e);
        }

    }
}
