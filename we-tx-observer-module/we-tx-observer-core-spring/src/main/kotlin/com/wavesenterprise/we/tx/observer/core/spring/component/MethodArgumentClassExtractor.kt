package com.wavesenterprise.we.tx.observer.core.spring.component

import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.we.tx.observer.api.key.KeyEvent
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

class MethodArgumentClassExtractor {
    companion object {

        fun extractClassFromMethodArgument(method: Method): Class<*> {
            require(method.genericParameterTypes.isNotEmpty())
            val clazz = getActualClassFromType(method.genericParameterTypes[0])
            return if (clazz != null &&
                (
                    Tx::class.java.isAssignableFrom(clazz) ||
                        KeyEvent::class.java.isAssignableFrom(clazz)
                    )
            ) {
                clazz
            } else {
                method.parameters[0].type
            }
        }

        private fun getActualClassFromType(type: Type?): Class<*>? = when (type) {
            is WildcardType -> extractClassFormWildcardType(type)
            is ParameterizedType -> extractClassFromParametrizedType(type)
            is Class<*> -> {
                if (Tx::class.java.isAssignableFrom(type)) {
                    type
                } else {
                    null
                }
            }
            else -> null
        }

        private fun extractClassFromParametrizedType(parametrizedType: ParameterizedType): Class<*>? =
            getActualClassFromType(parametrizedType.actualTypeArguments[0]) ?: parametrizedType.rawType as Class<*>

        private fun extractClassFormWildcardType(wildcardType: WildcardType): Class<*>? =
            getActualClassFromType(wildcardType.upperBounds[0])
    }
}
