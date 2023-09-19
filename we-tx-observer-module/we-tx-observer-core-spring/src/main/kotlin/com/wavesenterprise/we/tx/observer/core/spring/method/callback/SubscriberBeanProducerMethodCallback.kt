package com.wavesenterprise.we.tx.observer.core.spring.method.callback

import com.wavesenterprise.we.tx.observer.common.tx.subscriber.TxSubscriber
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Method

abstract class SubscriberBeanProducerMethodCallback(
    private val beanFactory: ConfigurableBeanFactory,
    private val beanName: String,
) : ReflectionUtils.MethodCallback {

    override fun doWith(method: Method) {
        val subscriber = buildSubscriber(method)
        beanFactory.registerSingleton(
            "${beanName}_${method.declaringClass.name}_${method.name}",
            subscriber
        )
    }

    protected abstract fun buildSubscriber(method: Method): TxSubscriber
}
