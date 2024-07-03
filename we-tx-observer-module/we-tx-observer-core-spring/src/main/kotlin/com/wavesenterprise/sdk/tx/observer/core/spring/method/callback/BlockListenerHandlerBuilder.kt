package com.wavesenterprise.sdk.tx.observer.core.spring.method.callback

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import com.wavesenterprise.sdk.node.domain.DataEntry
import com.wavesenterprise.sdk.node.domain.DataValue
import com.wavesenterprise.sdk.node.domain.tx.CallContractTx
import com.wavesenterprise.sdk.node.domain.tx.CreateContractTx
import com.wavesenterprise.sdk.node.domain.tx.ExecutedContractTx
import com.wavesenterprise.sdk.node.domain.tx.PolicyDataHashTx
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.domain.tx.UpdateContractTx
import com.wavesenterprise.sdk.tx.observer.api.BlockListenerHandlerException
import com.wavesenterprise.sdk.tx.observer.api.BlockListenerSingleTxHandlerException
import com.wavesenterprise.sdk.tx.observer.api.key.KeyEvent
import com.wavesenterprise.sdk.tx.observer.api.key.KeyFilter
import com.wavesenterprise.sdk.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.sdk.tx.observer.api.privacy.PrivateDataEvent
import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import com.wavesenterprise.sdk.tx.observer.common.tx.handler.TxHandler
import com.wavesenterprise.sdk.tx.observer.core.spring.method.callback.BlockListenerHandlerBuilder.Companion.logger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

@SuppressWarnings("TooManyFunctions")
class BlockListenerHandlerBuilder(
    private val beanInstance: Any,
    private val privateContentResolverProvider: ObjectProvider<PrivateContentResolver>,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(TxListener::class.java)
    }

    fun buildHandlerForMethod(method: Method): TxHandler {
        return ExceptionCatchingTxHandlerWrapper(
            methodName = method.toGenericString(),
            txHandler = buildSpecificHandler(method),
        )
    }

    private fun buildSpecificHandler(method: Method): TxHandler {
        return when {
            KeyEvent::class.java.isAssignableFrom(method.parameters[0].type) -> {
                handlerForKeyEvent(method)
            }
            PrivateDataEvent::class.java.isAssignableFrom(method.parameters[0].type) -> {
                handlerForPrivateDataEvent(method)
            }
            else -> {
                handlerForSingleTx(method)
            }
        }
    }

    private fun handlerForSingleTx(method: Method): TxHandler = object : TxHandler {
        override fun handle(tx: Tx) {
            wrapSingleTxInvocation(
                method = method,
                methodInvocation = { method.invoke(beanInstance, tx) },
                tx = tx,
            )
        }
    }

    private fun handlerForKeyEvent(method: Method): TxHandler = object : TxHandler {
        override fun handle(tx: Tx) {
            extractKeyEvents(tx as ExecutedContractTx, method).forEach {
                wrapSingleTxInvocation(
                    method = method,
                    methodInvocation = { method.invoke(beanInstance, it) },
                    tx = it.tx,
                )
            }
        }
    }

    private fun handlerForPrivateDataEvent(method: Method): TxHandler = object : TxHandler {
        override fun handle(tx: Tx) {
            extractPrivateDataEvent(tx as PolicyDataHashTx, method)
                .also {
                    wrapSingleTxInvocation(
                        method = method,
                        methodInvocation = { method.invoke(beanInstance, it) },
                        tx = it.policyDataHashTx,
                    )
                }
        }
    }

    private fun wrapSingleTxInvocation(
        method: Method,
        methodInvocation: () -> Unit,
        tx: Tx,
    ) = try {
        methodInvocation.invoke()
    } catch (ex: ReflectiveOperationException) {
        val causeException = ex.cause ?: ex
        val methodName = method.toGenericString()
        logger.error("Error invoking handler for method $methodName on TX with ID = '${tx.id}'", causeException)
        throw BlockListenerSingleTxHandlerException(
            txId = tx.id.asBase58String(),
            message = "Error invoking handler for method $methodName",
            cause = causeException,
        )
    }

    private fun extractKeyEvents(tx: ExecutedContractTx, method: Method): List<KeyEvent<*>> {
        require(method.genericParameterTypes.isNotEmpty())
        val type = (method.genericParameterTypes[0] as ParameterizedType).actualTypeArguments[0]
        val keyFilter = method.parameters[0].getAnnotation(KeyFilter::class.java)
        val paramsValues = getParamValue(keyFilter = keyFilter, paramList = tx.results)
        return paramsValues.map {
            KeyEvent(
                payload = getDeserializedValue(it.value.getValue().toString(), type),
                tx = tx,
                key = it.key.value,
                contractId = tx.contractId(),
            )
        }
    }

    private fun DataValue.getValue() =
        when (this) {
            is DataValue.IntegerDataValue -> value
            is DataValue.BooleanDataValue -> value
            is DataValue.BinaryDataValue -> value
            is DataValue.StringDataValue -> value
        }

    private fun test(dataValue: DataValue, type: Type): Any =
        when (dataValue) {
            is DataValue.IntegerDataValue -> dataValue.value
            is DataValue.BooleanDataValue -> dataValue.value
            is DataValue.BinaryDataValue -> dataValue.value
            is DataValue.StringDataValue -> {
                val typeFactory: TypeFactory = objectMapper.typeFactory
                val javaType: JavaType = typeFactory.constructType(type)
                objectMapper.readValue(dataValue.value, javaType)
            }
        }

    private fun ExecutedContractTx.contractId(): String =
        when (val tx = tx) {
            is CallContractTx -> tx.contractId.asBase58String()
            is CreateContractTx -> tx.id.asBase58String()
            is UpdateContractTx -> tx.contractId.asBase58String()
        }

    private fun extractPrivateDataEvent(tx: PolicyDataHashTx, method: Method): PrivateDataEvent<*> {
        require(method.genericParameterTypes.isNotEmpty())
        val parameter = method.genericParameterTypes[0]
        if (parameter is ParameterizedType) {
            return privateContentResolverProvider.getObject().decode<Any>(
                tx = tx,
                type = parameter.actualTypeArguments[0],
            )
        }
        return privateContentResolverProvider.getObject().decode(tx = tx)
    }

    private fun getDeserializedValue(value: String, type: Type): Any {
        return when (type) {
            is Class<*> -> getValueFromClass(value, type)
            else -> getValueFromType(value, type)
        }
    }

    private fun getValueFromClass(value: String, type: Class<*>): Any {
        return when {
            String::class.java.isAssignableFrom(type) -> {
                value
            }
            java.lang.Boolean::class.java.isAssignableFrom(type) -> {
                value.toBoolean()
            }
            Long::class.java.isAssignableFrom(type) || Int::class.java.isAssignableFrom(type) -> {
                value.toLong()
            }
            else -> {
                objectMapper.readValue(value, type)
            }
        }
    }

    private fun getValueFromType(value: String, type: Type): Any {
        val typeFactory: TypeFactory = objectMapper.typeFactory
        val javaType: JavaType = typeFactory.constructType(type)
        return objectMapper.readValue(value, javaType)
    }

    private fun getParamValue(
        keyFilter: KeyFilter,
        paramList: List<DataEntry>,
    ): List<DataEntry> =
        (
            if (keyFilter.keyPrefix.isNotBlank()) {
                paramList.filter { it.key.value.startsWith(keyFilter.keyPrefix) }
            } else {
                paramList.filter { it.key.value.matches(Regex(keyFilter.keyRegexp)) }
            }
            )
}

internal class ExceptionCatchingTxHandlerWrapper(
    private val methodName: String,
    private val txHandler: TxHandler,
) : TxHandler {
    override fun handle(tx: Tx) {
        try {
            txHandler.handle(tx)
        } catch (ex: ReflectiveOperationException) {
            val causeException = ex.cause ?: ex
            logger.error("Error invoking handler for method $methodName", causeException)
            throw BlockListenerHandlerException("Error invoking handler for method $methodName", causeException)
        }
    }
}
