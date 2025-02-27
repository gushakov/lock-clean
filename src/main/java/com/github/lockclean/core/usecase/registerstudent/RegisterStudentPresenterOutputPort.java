package com.github.lockclean.core.usecase.registerstudent;

import com.github.lockclean.core.model.student.Student;
import com.github.lockclean.core.port.ErrorHandlingPresenterOutputPort;

public interface RegisterStudentPresenterOutputPort extends ErrorHandlingPresenterOutputPort {
    void presentSuccessfulResultOfRegisteringNewStudent(Student student);
}
