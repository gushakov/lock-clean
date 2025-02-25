package com.github.lockclean.core.usecase.subscribestudent;

public interface SubscribeStudentInputPort {

    void facultySecretarySubscribesStudentToCourse(String studentIdArg, String courseIdArg);

}
