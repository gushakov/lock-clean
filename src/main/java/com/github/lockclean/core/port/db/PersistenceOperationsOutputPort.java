package com.github.lockclean.core.port.db;

import com.github.lockclean.core.model.course.Course;
import com.github.lockclean.core.model.course.CourseId;
import com.github.lockclean.core.model.student.Student;
import com.github.lockclean.core.model.student.StudentId;
import com.github.lockclean.core.model.subscription.Subscription;

public interface PersistenceOperationsOutputPort {
    int countNumberOfSubscribersToCourse(CourseId courseId);

    Course obtainCourseById(CourseId courseId);

    int countNumberOfCoursesSubscribedForByStudent(StudentId studentId);

    Student obtainStudentById(StudentId studentId);

    void saveCourse(Course course);

    void saveStudent(Student student);

    void saveSubscription(Subscription subscription);
}
