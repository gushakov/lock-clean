package com.github.lockclean.infrastructure.adapter.db.student;

import org.springframework.data.repository.CrudRepository;

public interface StudentDbEntityRepository extends CrudRepository<StudentDbEntity, String> {
}
