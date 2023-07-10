package com.wavesenterprise.we.tx.observer.core.spring.method.callback

import com.fasterxml.jackson.databind.ObjectMapper
import com.wavesenterprise.we.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.we.tx.observer.common.tx.subscriber.TxSubscriber
import com.wavesenterprise.we.tx.observer.common.tx.subscriber.TxSubscriberImpl
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.expression.BeanFactoryResolver
import java.lang.reflect.Method

class BlockListenerMethodCallback(
    beanFactory: ConfigurableBeanFactory,
    private val beanInstance: Any,
    private val beanFactoryResolver: BeanFactoryResolver,
    private val privateContentResolverProvider: ObjectProvider<PrivateContentResolver>,
    private val objectMapper: ObjectMapper,
) : SubscriberBeanProducerMethodCallback(beanFactory, BLOCK_LISTENER_BEAN_NAME_PREFIX) {

    override fun buildSubscriber(method: Method): TxSubscriber {
        return TxSubscriberImpl(
            predicate = BlockListenerPredicateBuilder(
                beanFactoryResolver = beanFactoryResolver,
                privateContentResolverProvider = privateContentResolverProvider,
            ).buildPredicate(method),
            txHandlers = listOf(
                BlockListenerHandlerBuilder(
                    beanInstance = beanInstance,
                    privateContentResolverProvider = privateContentResolverProvider,
                    objectMapper = objectMapper,
                ).buildHandlerForMethod(method)
            )
        )
    }

    companion object {
        const val BLOCK_LISTENER_BEAN_NAME_PREFIX = "blockListener"
    }
}
