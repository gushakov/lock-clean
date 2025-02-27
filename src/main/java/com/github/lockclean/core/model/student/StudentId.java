package com.github.lockclean.core.model.student;

import com.github.lockclean.core.model.Validator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

@Value
public class StudentId {

    @Getter(AccessLevel.NONE)
    String id;

    public StudentId(String id) {
        this.id = Validator.notNullOrBlank(id);
    }

    public String asString() {
        return id;
    }
}
