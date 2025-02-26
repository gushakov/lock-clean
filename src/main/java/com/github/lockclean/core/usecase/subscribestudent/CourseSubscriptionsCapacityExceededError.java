package com.github.lockclean.core.usecase.subscribestudent;

import com.github.lockclean.core.GenericLockCleanError;
import com.github.lockclean.core.model.course.CourseId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/*
    POINT OF INTEREST
    -----------------
    Business exception thrown by the subscription use case when
    course's capacity is exceeded.
 */

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CourseSubscriptionsCapacityExceededError extends GenericLockCleanError {

    CourseId courseId;
    Integer capacity;

    public CourseSubscriptionsCapacityExceededError(CourseId courseId, Integer capacity) {
        super("Course %s has reached its current capacity of %d and cannot accept any more subscriptions"
                .formatted(courseId.asString(), capacity));
        this.courseId = courseId;
        this.capacity = capacity;
    }
}
