package com.github.lockclean.core.port.id;

import com.github.lockclean.core.model.course.CourseId;
import com.github.lockclean.core.model.student.StudentId;
import com.github.lockclean.core.model.subscription.SubscriptionId;

public interface IdsOperationsOutputPort {

    CourseId generateNewCourseId();

    StudentId generateNewStudentId();

    SubscriptionId generateNewSubscriptionId();

}
