package com.github.lockclean.core.usecase.subscribestudent;

public interface SubscribeStudentInputPort {

    /**
     * Maximum number of courses a student may subscribe for.
     */
    int STUDENT_SUBSCRIPTIONS_LIMIT = 10;

    /**
     * System subscribes a student for a course. We assume that this use case
     * will be executed in a parallelized environnement with multiple concurrent
     * threads executing this method at once. This can correspond to running
     * in a batch mode, for example.
     *
     * @param studentIdArg ID of a student
     * @param courseIdArg  ID of a course
     * @param optional whether this is a subscription to an optional course
     *                 (in the program-of-study for the student)
     */
    void subscribeStudentToCourse(String studentIdArg, String courseIdArg, boolean optional);

}
