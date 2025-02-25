package com.github.lockclean.core.usecase.subscribestudent;

import com.github.lockclean.core.model.course.CourseId;
import com.github.lockclean.core.model.student.StudentId;
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
    public void facultySecretarySubscribesStudentToCourse(String studentIdArg, String courseIdArg) {
        try {

            // prepare and validate input IDs
            CourseId courseId = new CourseId(courseIdArg);
            StudentId studentId = new StudentId(studentIdArg);



        }
        catch (Exception e) {
            presenter.presentError(e);
        }
    }
}
