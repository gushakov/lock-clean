package com.github.lockclean.infrastructure.adapter.db.subscription;

import org.springframework.data.repository.CrudRepository;

public interface SubscriptionDbEntityRepository extends CrudRepository<SubscriptionDbEntity, String> {

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
