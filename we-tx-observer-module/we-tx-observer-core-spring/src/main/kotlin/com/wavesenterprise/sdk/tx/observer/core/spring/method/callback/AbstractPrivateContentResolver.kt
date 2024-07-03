package com.wavesenterprise.sdk.tx.observer.core.spring.method.callback

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.node.client.blocking.privacy.PrivacyService
import com.wavesenterprise.sdk.node.client.blocking.tx.TxService
import com.wavesenterprise.sdk.node.domain.privacy.PolicyItemInfoResponse
import com.wavesenterprise.sdk.node.domain.privacy.PolicyItemRequest
import com.wavesenterprise.sdk.node.domain.tx.CreatePolicyTx
import com.wavesenterprise.sdk.node.domain.tx.PolicyDataHashTx
import com.wavesenterprise.sdk.tx.observer.api.privacy.PrivateContentResolver
import java.lang.reflect.Type
import java.util.Base64

abstract class AbstractPrivateContentResolver(
    private val nodeBlockingServiceFactory: NodeBlockingServiceFactory,
    private val objectMapper: ObjectMapper = jacksonObjectMapper(),
) : PrivateContentResolver {

    private val txService: TxService = nodeBlockingServiceFactory.txService()
    private val privacyService: PrivacyService = nodeBlockingServiceFactory.privacyService()

    override fun getPolicyTx(tx: PolicyDataHashTx): CreatePolicyTx =
        checkNotNull(txService.txInfo(tx.policyId.txId).orElseGet(null)) {
            "No info about policy with id=${tx.policyId} in blockchain"
        }.tx as CreatePolicyTx

    override fun getPolicyName(tx: PolicyDataHashTx): String =
        getPolicyTx(tx).policyName.value

    protected fun getPrivacyInfo(tx: PolicyDataHashTx): PolicyItemInfoResponse =
        checkNotNull(
            privacyService.info(
                PolicyItemRequest(
                    policyId = tx.policyId,
                    dataHash = tx.dataHash,
                ),
            ).orElseGet(null),
        ) {
            "No info about item with policyId=${tx.policyId} and dataHash=${tx.dataHash} in privacy"
        }

    protected fun getPrivacyData(tx: PolicyDataHashTx): ByteArray {
        val data = checkNotNull(
            privacyService.data(
                PolicyItemRequest(
                    policyId = tx.policyId,
                    dataHash = tx.dataHash,
                ),
            ).orElseGet(null),
        ) {
            "No data for item with policyId=${tx.policyId} and dataHash=${tx.dataHash} in privacy"
        }
        return Base64.getDecoder().decode(data.bytes)
    }

    protected fun mapPayload(bytes: ByteArray, type: Type): Any {
        when (type) {
            ByteArray::class.java ->
                return bytes
            String::class.java ->
                return String(bytes)
            else -> {
                val constructedType = objectMapper.typeFactory.constructType(type)
                return objectMapper.readValue(String(bytes), constructedType)
            }
        }
    }
}
