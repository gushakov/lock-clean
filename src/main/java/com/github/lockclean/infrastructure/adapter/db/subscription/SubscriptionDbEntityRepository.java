package com.github.lockclean.infrastructure.adapter.db.subscription;

import org.springframework.data.repository.CrudRepository;

public interface SubscriptionDbEntityRepository extends CrudRepository<SubscriptionDbEntity, String> {

    /**
     * Returns {@code true} is a subscription with matching student ID and course ID
     * exists already.
     * @param studentId ID of student
     * @param courseId ID of course
     * @return {@code true} is a subscription exists
     */
    boolean existsByStudentIdAndCourseId(String studentId, String courseId);

    /**
     * Count number of entries in {@code subscription} table with matching {@code courseId}.
     * @param courseId ID of a course
     * @return number of subscriptions
     */
    int countByCourseId(String courseId);

    /**
     * Count number of entries in {@code subscription} table with matching {@code studentId}.
     * @param studentId ID of a student
     * @return number of subscriptions
     */
    int countByStudentId(String studentId);

}
