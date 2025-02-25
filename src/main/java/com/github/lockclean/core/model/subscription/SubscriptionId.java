package com.github.lockclean.core.model.subscription;

import com.github.lockclean.core.model.Validator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

@Value
public class SubscriptionId {

    @Getter(AccessLevel.NONE)
    String id;

    public SubscriptionId(String id) {
        this.id = Validator.notNullOrBlank(id);
    }

    public String asString(){
        return id;
    }
}
