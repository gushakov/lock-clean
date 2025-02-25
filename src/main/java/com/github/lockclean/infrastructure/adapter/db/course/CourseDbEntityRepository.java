package com.github.lockclean.infrastructure.adapter.db.course;

import org.springframework.data.repository.CrudRepository;

public interface CourseDbEntityRepository extends CrudRepository<CourseDbEntity, String> {
}
