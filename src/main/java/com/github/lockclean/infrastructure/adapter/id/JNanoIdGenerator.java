package com.github.lockclean.infrastructure.adapter.id;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class JNanoIdGenerator {

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int LENGTH = 6;

    public String generateCourseId() {
        return "crs_" + NanoIdUtils.randomNanoId(RANDOM, ALPHABET, LENGTH);
    }
    public String generateStudentId() {
        return "stu_" + NanoIdUtils.randomNanoId(RANDOM, ALPHABET, LENGTH);
    }
    public String generateSubscriptionId() {
        return "sub_" + NanoIdUtils.randomNanoId(RANDOM, ALPHABET, LENGTH);
    }

}
