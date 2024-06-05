package com.wavesenterprise.sdk.tx.observer.starter.annotation

import com.fasterxml.jackson.databind.ObjectMapper
import com.wavesenterprise.sdk.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.sdk.tx.observer.core.spring.method.callback.BlockListenerMethodCallback
import com.wavesenterprise.sdk.tx.observer.core.spring.method.filter.BlockListenerMethodFilter
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.expression.BeanFactoryResolver
import org.springframework.util.ReflectionUtils

class EventListenerAnnotationProcessor(
    private val beanFactory: ConfigurableBeanFactory,
    private val beanFactoryResolver: BeanFactoryResolver,
    private val privateContentResolverProvider: ObjectProvider<PrivateContentResolver>,
    private val objectMapper: ObjectMapper,
) : BeanPostProcessor {

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        // todo check for annotations and build tx-observer objects by annotation metadata ?
        val blockListenerMethodCallback = BlockListenerMethodCallback(
            beanFactory = beanFactory,
            beanInstance = bean,
            beanFactoryResolver = beanFactoryResolver,
            privateContentResolverProvider = privateContentResolverProvider,
            objectMapper = objectMapper,
        )
        ReflectionUtils.doWithMethods(
            bean.javaClass,
            blockListenerMethodCallback,
            BlockListenerMethodFilter(),
        )
        return bean
    }
}
