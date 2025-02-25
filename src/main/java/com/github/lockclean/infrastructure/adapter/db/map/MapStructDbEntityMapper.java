package com.github.lockclean.infrastructure.adapter.db.map;

import com.github.lockclean.core.model.course.Course;
import com.github.lockclean.core.model.student.Student;
import com.github.lockclean.core.model.subscription.Subscription;
import com.github.lockclean.infrastructure.adapter.db.course.CourseDbEntity;
import com.github.lockclean.infrastructure.adapter.db.student.StudentDbEntity;
import com.github.lockclean.infrastructure.adapter.db.subscription.SubscriptionDbEntity;
import org.mapstruct.Mapper;

@Mapper
public abstract class MapStructDbEntityMapper implements DbEntityMapper {
    @Override
    public abstract CourseDbEntity map(Course course);

    @Override
    public abstract Course map(CourseDbEntity courseDbEntity);

    @Override
    public abstract StudentDbEntity map(Student student);

    @Override
    public abstract Student map(StudentDbEntity studentDbEntity);

    @Override
    public abstract SubscriptionDbEntity map(Subscription subscription);

    @Override
    public abstract Subscription map(SubscriptionDbEntity subscriptionDbEntity);
}
