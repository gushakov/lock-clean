package com.github.lockclean.infrastructure.adapter.id;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.github.lockclean.core.model.course.CourseId;
import com.github.lockclean.core.model.student.StudentId;
import com.github.lockclean.core.model.subscription.SubscriptionId;
import com.github.lockclean.core.port.id.IdsOperationsOutputPort;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class JNanoIdGenerator implements IdsOperationsOutputPort {

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int LENGTH = 6;

    @Override
    public CourseId generateNewCourseId() {
        return new CourseId("crs_" + NanoIdUtils.randomNanoId(RANDOM, ALPHABET, LENGTH));
    }

    @Override
    public StudentId generateNewStudentId() {
        return new StudentId("stu_" + NanoIdUtils.randomNanoId(RANDOM, ALPHABET, LENGTH));
    }

    @Override
    public SubscriptionId generateNewSubscriptionId() {
        return new SubscriptionId("sub_" + NanoIdUtils.randomNanoId(RANDOM, ALPHABET, LENGTH));
    }
}
