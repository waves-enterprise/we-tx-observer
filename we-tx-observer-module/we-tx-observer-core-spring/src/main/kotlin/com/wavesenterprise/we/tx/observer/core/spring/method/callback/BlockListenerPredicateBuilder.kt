package com.wavesenterprise.we.tx.observer.core.spring.method.callback

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.wavesenterprise.sdk.node.domain.TxType
import com.wavesenterprise.sdk.node.domain.tx.AtomicTx
import com.wavesenterprise.sdk.node.domain.tx.BurnTx
import com.wavesenterprise.sdk.node.domain.tx.CallContractTx
import com.wavesenterprise.sdk.node.domain.tx.ContractTx
import com.wavesenterprise.sdk.node.domain.tx.CreateAliasTx
import com.wavesenterprise.sdk.node.domain.tx.CreateContractTx
import com.wavesenterprise.sdk.node.domain.tx.CreatePolicyTx
import com.wavesenterprise.sdk.node.domain.tx.DataTx
import com.wavesenterprise.sdk.node.domain.tx.DisableContractTx
import com.wavesenterprise.sdk.node.domain.tx.ExecutedContractTx
import com.wavesenterprise.sdk.node.domain.tx.GenesisPermitTx
import com.wavesenterprise.sdk.node.domain.tx.GenesisRegisterNodeTx
import com.wavesenterprise.sdk.node.domain.tx.GenesisTx
import com.wavesenterprise.sdk.node.domain.tx.IssueTx
import com.wavesenterprise.sdk.node.domain.tx.LeaseCancelTx
import com.wavesenterprise.sdk.node.domain.tx.LeaseTx
import com.wavesenterprise.sdk.node.domain.tx.MassTransferTx
import com.wavesenterprise.sdk.node.domain.tx.PermitTx
import com.wavesenterprise.sdk.node.domain.tx.PolicyDataHashTx
import com.wavesenterprise.sdk.node.domain.tx.RegisterNodeTx
import com.wavesenterprise.sdk.node.domain.tx.ReissueTx
import com.wavesenterprise.sdk.node.domain.tx.SetAssetScriptTx
import com.wavesenterprise.sdk.node.domain.tx.SetScriptTx
import com.wavesenterprise.sdk.node.domain.tx.TransferTx
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.domain.tx.Tx.Companion.type
import com.wavesenterprise.sdk.node.domain.tx.UpdateContractTx
import com.wavesenterprise.sdk.node.domain.tx.UpdatePolicyTx
import com.wavesenterprise.we.tx.observer.api.key.KeyEvent
import com.wavesenterprise.we.tx.observer.api.key.KeyFilter
import com.wavesenterprise.we.tx.observer.api.privacy.MessageFilter
import com.wavesenterprise.we.tx.observer.api.privacy.MessageFilters
import com.wavesenterprise.we.tx.observer.api.privacy.PolicyFilter
import com.wavesenterprise.we.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.we.tx.observer.api.privacy.PrivateDataEvent
import com.wavesenterprise.we.tx.observer.api.tx.TxListener
import com.wavesenterprise.we.tx.observer.core.spring.component.MethodArgumentClassExtractor.Companion.extractClassFromMethodArgument
import com.wavesenterprise.we.tx.observer.core.spring.util.TxSpElUtils
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.expression.BeanFactoryResolver
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.EnumSet

class BlockListenerPredicateBuilder(
    private val beanFactoryResolver: BeanFactoryResolver,
    private val privateContentResolverProvider: ObjectProvider<PrivateContentResolver>,
) {

    fun buildPredicate(method: Method): (Tx) -> Boolean {
        // todo throw internal exception for all problems and validations - catch and write error with info about method
        val annotation = requireNotNull(method.getAnnotation(TxListener::class.java)) {
            "Annotation TxListener is not present on the method"
        }
        require(method.parameterCount == 1) {
            "TxListener annotated methods supports only a single arg!"
        }
        val methodArgType = extractClassFromMethodArgument(method)
        val argumentTypePredicate: (Tx) -> Boolean = when {
            KeyEvent::class.java.isAssignableFrom(methodArgType) -> buildKeyEventPredicate(method)
            PrivateDataEvent::class.java.isAssignableFrom(methodArgType) -> buildDataEventPredicate(method)
            Tx::class.java.isAssignableFrom(methodArgType) -> getTxTypePredicate(methodArgType)
            else -> throw IllegalArgumentException(
                "TxListener annotated method can have arguments only of type Tx, KeyEvent or PrivateDataEvent"
            )
        }
        val expressionPredicate = buildExpressionPredicate(annotation)
        return { argumentTypePredicate.invoke(it) && expressionPredicate.invoke(it) }
    }

    private fun buildKeyEventPredicate(method: Method): (Tx) -> Boolean {
        val keyFilter = validateAndGetKeyFilter(method.parameters[0])
        return if (keyFilter.keyRegexp.isNotBlank()) {
            { tx: Tx ->
                TxType.EXECUTED_CONTRACT == tx.type() &&
                    TxSpElUtils.hasKeyWithRegex(tx as ExecutedContractTx, keyFilter.keyRegexp)
            }
        } else {
            { tx: Tx ->
                TxType.EXECUTED_CONTRACT == tx.type() &&
                    TxSpElUtils.hasKeyWithPrefix(tx as ExecutedContractTx, keyFilter.keyPrefix)
            }
        }
    }

    private fun buildDataEventPredicate(method: Method): (Tx) -> Boolean {
        val policyPredicate = buildPolicyPredicate(method)
        val messagePredicates = buildMessagePredicates(method)
        return { tx: Tx ->
            TxType.POLICY_DATA_HASH == tx.type() &&
                policyPredicate.invoke(tx as PolicyDataHashTx) &&
                messagePredicates.invoke(tx)
        }
    }

    private fun buildPolicyPredicate(method: Method): (PolicyDataHashTx) -> Boolean {
        val policyFilter = validateAndGetPolicyFilter(method.parameters[0])
        return policyFilter?.let {
            if (it.nameRegExp.isNotBlank()) {
                { tx: PolicyDataHashTx ->
                    TxSpElUtils.isMatchRegex(
                        value = privateContentResolverProvider.getPolicyName(tx),
                        regExp = it.nameRegExp,
                    )
                }
            } else {
                { tx: PolicyDataHashTx ->
                    TxSpElUtils.isWithPrefix(
                        value = privateContentResolverProvider.getPolicyName(tx),
                        prefix = it.namePrefix,
                    )
                }
            }
        } ?: {
            true
        }
    }

    private fun buildMessagePredicates(method: Method): (PolicyDataHashTx) -> Boolean {
        val messageFilters = validateAndGetMessageFilters(method.parameters[0])
        val predicates = messageFilters.map { filter ->
            if (filter.metaKeyValueRegExp.isNotBlank()) {
                { meta: JsonNode ->
                    when (val node = meta[filter.metaKey]) {
                        is NullNode, null -> false
                        else -> TxSpElUtils.isMatchRegex(node.asText(), filter.metaKeyValueRegExp)
                    }
                }
            } else {
                { meta: JsonNode ->
                    when (val node = meta[filter.metaKey]) {
                        is NullNode, null -> false
                        else -> node.asText() == filter.metaKeyValue
                    }
                }
            }
        }
        return { tx: PolicyDataHashTx ->
            if (predicates.isEmpty()) {
                true
            } else {
                val meta = privateContentResolverProvider.getMeta(tx)
                meta != null && predicates.all { it.invoke(meta) }
            }
        }
    }

    private fun validateAndGetPolicyFilter(methodParameter: Parameter): PolicyFilter? {
        return methodParameter.getAnnotation(PolicyFilter::class.java)
            ?.also {
                require(it.namePrefix.isNotBlank() || it.nameRegExp.isNotBlank()) {
                    "One of the namePrefix or nameRegExp properties should be specified for PolicyFilter"
                }
            }
    }

    private fun validateAndGetMessageFilters(methodParameter: Parameter): Array<MessageFilter> {
        val messageFilters =
            (
                methodParameter.getAnnotation(MessageFilter::class.java)?.let { arrayOf(it) }
                    ?: arrayOf()
                ) +
                (
                    methodParameter.getAnnotation(MessageFilters::class.java)?.filters
                        ?: arrayOf()
                    )

        messageFilters.forEach {
            require(it.metaKey.isNotBlank() && it.metaKeyValue.isNotBlank() || it.metaKeyValueRegExp.isNotBlank()) {
                "MetaKey and one of the metaKeyValue or metaKeyValueRegExp properties should be specified for MessageFilter"
            }
        }
        return messageFilters
    }

    private fun getTxTypePredicate(methodArgType: Class<*>): (Tx) -> Boolean {
        val txTypeCodes = getAllowedTxTypes(methodArgType).map { it }.toSet()
        return if (txTypeCodes.isEmpty()) {
            if (ExecutedContractTx::class.java.isAssignableFrom(methodArgType)) {
                { tx: Tx -> tx.type() == TxType.EXECUTED_CONTRACT }
            } else {
                { true }
            }
        } else {
            when {
                txTypeCodes.contains(TxType.CREATE_CONTRACT) ||
                    txTypeCodes.contains(TxType.CALL_CONTRACT) -> {
                    { tx: Tx ->
                        tx.type() == TxType.EXECUTED_CONTRACT &&
                            ((tx as ExecutedContractTx).tx.type() in txTypeCodes)
                    }
                }
                else -> {
                    { it.type() in txTypeCodes }
                }
            }
        }
    }

    private fun getAllowedTxTypes(clazz: Class<*>): EnumSet<TxType> =
        if (Tx::class.java.isAssignableFrom(clazz)) {
            // todo match with jsonSubTypes from Tx
            when {
                ContractTx::class.java.isAssignableFrom(clazz) -> getAllowedForContractTx(clazz)
                CreatePolicyTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.CREATE_POLICY)
                UpdatePolicyTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.UPDATE_POLICY)
                PolicyDataHashTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.POLICY_DATA_HASH)
                AtomicTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.ATOMIC)
                GenesisTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.GENESIS)
                IssueTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.ISSUE)
                TransferTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.TRANSFER)
                ReissueTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.REISSUE)
                BurnTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.BURN)
                LeaseTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.LEASE)
                LeaseCancelTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.LEASE_CANCEL)
                CreateAliasTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.CREATE_ALIAS)
                MassTransferTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.MASS_TRANSFER)
                DataTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.DATA)
                SetScriptTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.SET_SCRIPT)
                SetAssetScriptTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.SET_ASSET_SCRIPT)
                GenesisPermitTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.GENESIS_PERMIT)
                PermitTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.PERMIT)
                DisableContractTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.DISABLE_CONTRACT)
                UpdateContractTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.UPDATE_CONTRACT)
                GenesisRegisterNodeTx::class.java.isAssignableFrom(clazz) ->
                    EnumSet.of(TxType.GENESIS_REGISTER_NODE)
                RegisterNodeTx::class.java.isAssignableFrom(clazz) -> EnumSet.of(TxType.REGISTER_NODE)
                else -> EnumSet.noneOf(TxType::class.java)
            }
        } else {
            EnumSet.noneOf(null)
        }

    private fun getAllowedForContractTx(clazz: Class<*>): EnumSet<TxType> {
        return when {
            CreateContractTx::class.java.isAssignableFrom(clazz) -> {
                EnumSet.of(TxType.CREATE_CONTRACT)
            }
            CallContractTx::class.java.isAssignableFrom(clazz) -> {
                EnumSet.of(TxType.CALL_CONTRACT)
            }
            else -> {
                EnumSet.of(TxType.CALL_CONTRACT, TxType.CREATE_CONTRACT)
            }
        }
    }

    private fun validateAndGetKeyFilter(methodParameter: Parameter): KeyFilter {
        val keyFilter = methodParameter.getAnnotation(KeyFilter::class.java)
            ?: throw
            IllegalArgumentException("VsKeyEvent typed method arguments should be annotated with @KeyFilter")
        require(keyFilter.keyPrefix.isNotBlank() || keyFilter.keyRegexp.isNotBlank()) {
            "One of the keyRegexp or keyPrefix properties should be specified for KeyFilter"
        }
        return keyFilter
    }

    private fun buildExpressionPredicate(annotation: TxListener): (Tx) -> Boolean {
        return if (annotation.filterExpression.isEmpty()) {
            { true }
        } else {
            { tx ->
                SpelExpressionParser()
                    .parseExpression(annotation.filterExpression)
                    .getValue(buildEvalContext(tx)) as Boolean
            }
        }
    }

    private fun buildEvalContext(tx: Tx) = StandardEvaluationContext(tx)
        .apply {
            TxSpElUtils::class.java.declaredMethods
                .forEach {
                    registerFunction(it.name, it)
                }
            setBeanResolver(beanFactoryResolver)
        }

    private fun ObjectProvider<PrivateContentResolver>.getPolicyName(tx: PolicyDataHashTx) =
        getObject().getPolicyName(tx)

    private fun ObjectProvider<PrivateContentResolver>.getMeta(tx: PolicyDataHashTx) =
        getObject().getMeta(tx)
}
