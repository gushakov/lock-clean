package com.github.lockclean.core.model;

import java.util.Optional;
import java.util.function.Predicate;

public class Validator {

    public static <T> T notNull(T attribute) {
        return Optional.ofNullable(attribute)
                .orElseThrow(() -> new InvalidDomainObjectError("Attribute must not be null"));
    }

    public static String notNullOrBlank(String attribute) {
        return Optional.ofNullable(attribute)
                .filter(Predicate.not(String::isBlank))
                .orElseThrow(() -> new InvalidDomainObjectError("String attribute must not be null or blank"));
    }

}
