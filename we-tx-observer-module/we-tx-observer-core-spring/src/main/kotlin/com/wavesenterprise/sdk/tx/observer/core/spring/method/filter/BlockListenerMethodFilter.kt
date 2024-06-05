package com.wavesenterprise.sdk.tx.observer.core.spring.method.filter

import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Method

class BlockListenerMethodFilter : ReflectionUtils.MethodFilter {
    override fun matches(method: Method): Boolean =
        AnnotationUtils.getAnnotation(method, TxListener::class.java) != null
}
