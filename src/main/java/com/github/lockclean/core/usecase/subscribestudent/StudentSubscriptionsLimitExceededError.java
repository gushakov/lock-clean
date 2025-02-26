package com.github.lockclean.core.usecase.subscribestudent;

import com.github.lockclean.core.GenericLockCleanError;
import com.github.lockclean.core.model.student.StudentId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/*
    POINT OF INTEREST
    -----------------
    Business exception thrown by the subscription use case when
    the limit for a student's subscriptions is exceeded.
 */

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class StudentSubscriptionsLimitExceededError extends GenericLockCleanError {

    StudentId studentId;

    public StudentSubscriptionsLimitExceededError(StudentId studentId) {
        super("Subscriptions for student with ID %s exceed current limit of %d".formatted(studentId.asString(),
                SubscribeStudentInputPort.STUDENT_SUBSCRIPTIONS_LIMIT));
        this.studentId = studentId;
    }
}
