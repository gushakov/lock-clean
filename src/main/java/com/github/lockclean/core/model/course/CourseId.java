package com.github.lockclean.core.model.course;

import com.github.lockclean.core.model.Validator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

@Value
public class CourseId {

    @Getter(AccessLevel.NONE)
    String id;

    public CourseId(String id) {
        this.id = Validator.notNullOrBlank(id);
    }

    public String asString(){
        return id;
    }
}
