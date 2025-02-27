package com.github.lockclean.core.port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ErrorHandlingPresenterOutputPort {
    Logger log = LoggerFactory.getLogger(ErrorHandlingPresenterOutputPort.class);

    default void presentError(Exception e) {
        log.error("Some unknown error has occurred", e);
    }
}
