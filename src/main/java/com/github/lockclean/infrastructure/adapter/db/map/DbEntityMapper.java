package com.github.lockclean.infrastructure.adapter.db.map;

import com.github.lockclean.core.model.course.Course;
import com.github.lockclean.core.model.course.CourseId;
import com.github.lockclean.core.model.student.Student;
import com.github.lockclean.core.model.student.StudentId;
import com.github.lockclean.core.model.subscription.Subscription;
import com.github.lockclean.core.model.subscription.SubscriptionId;
import com.github.lockclean.infrastructure.adapter.db.course.CourseDbEntity;
import com.github.lockclean.infrastructure.adapter.db.student.StudentDbEntity;
import com.github.lockclean.infrastructure.adapter.db.subscription.SubscriptionDbEntity;

public interface DbEntityMapper {
    default String convertCourseIdToString(CourseId courseId) {
        if (courseId == null) {
            return null;
        }
        return courseId.asString();
    }

    default CourseId convertStringToCourseId(final String courseId) {
        if (courseId == null) {
            return null;
        }
        return new CourseId(courseId);
    }

    default String convertStudentIdToString(final StudentId studentId) {
        if (studentId == null) {
            return null;
        }
        return studentId.asString();
    }

    default StudentId convertStringToStudentId(final String studentId) {
        if (studentId == null) {
            return null;
        }
        return new StudentId(studentId);
    }

    default String convertSubscriptionIdToString(final SubscriptionId subscriptionId) {
        if (subscriptionId == null) {
            return null;
        }
        return subscriptionId.asString();
    }

    default SubscriptionId convertStringToSubscriptionId(final String subscriptionId) {
        if (subscriptionId == null) {
            return null;
        }
        return new SubscriptionId(subscriptionId);
    }

    CourseDbEntity map(Course course);

    Course map(CourseDbEntity courseDbEntity);

    StudentDbEntity map(Student student);

    Student map(StudentDbEntity studentDbEntity);

    SubscriptionDbEntity map(Subscription subscription);

    Subscription map(SubscriptionDbEntity subscriptionDbEntity);
}
