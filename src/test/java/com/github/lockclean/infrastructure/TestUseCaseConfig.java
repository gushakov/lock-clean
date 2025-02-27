package com.github.lockclean.infrastructure;

import com.github.lockclean.core.port.db.PersistenceOperationsOutputPort;
import com.github.lockclean.core.port.id.IdsOperationsOutputPort;
import com.github.lockclean.core.port.transaction.TransactionOperationsOutputPort;
import com.github.lockclean.core.usecase.registercourse.RegisterCourseInputPort;
import com.github.lockclean.core.usecase.registercourse.RegisterCourseUseCase;
import com.github.lockclean.core.usecase.registerstudent.RegisterStudentInputPort;
import com.github.lockclean.core.usecase.registerstudent.RegisterStudentUseCase;
import com.github.lockclean.core.usecase.subscribestudent.SubscribeStudentInputPort;
import com.github.lockclean.core.usecase.subscribestudent.SubscribeStudentUseCase;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class TestUseCaseConfig {

    /*
        POINT OF INTEREST
        -----------------
        We are declaring all the prototype beans for all our use cases needed
        for our test. For tests, we are using the logging versions of presenters
        (console output only).
     */

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RegisterCourseInputPort registerCourseUseCase(TransactionOperationsOutputPort txOps,
                                                         PersistenceOperationsOutputPort persistenceOps,
                                                         IdsOperationsOutputPort idsOps) {
        return new RegisterCourseUseCase(new RegisterCourseLoggingPresenter(), txOps, persistenceOps, idsOps);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RegisterStudentInputPort registerStudentUseCase(TransactionOperationsOutputPort txOps,
                                                           PersistenceOperationsOutputPort persistenceOps,
                                                           IdsOperationsOutputPort idsOps) {
        return new RegisterStudentUseCase(new RegisterStudentLoggingPresenter(), txOps, persistenceOps, idsOps);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SubscribeStudentInputPort subscribeStudentUseCase(TransactionOperationsOutputPort txOps,
                                                             PersistenceOperationsOutputPort persistenceOps,
                                                             IdsOperationsOutputPort idsOps) {
        return new SubscribeStudentUseCase(new SubscribeStudentLoggingPresenter(),
                txOps, persistenceOps, idsOps);
    }

}
