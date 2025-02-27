package com.github.lockclean.infrastructure;

import com.github.lockclean.core.model.student.Student;
import com.github.lockclean.core.usecase.registerstudent.RegisterStudentPresenterOutputPort;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegisterStudentLoggingPresenter implements RegisterStudentPresenterOutputPort {
    @Override
    public void presentSuccessfulResultOfRegisteringNewStudent(Student student) {
        log.info("Registered new student with full name: {}, ID: {}", student.getFullName(), student.getId().asString());
    }
}
