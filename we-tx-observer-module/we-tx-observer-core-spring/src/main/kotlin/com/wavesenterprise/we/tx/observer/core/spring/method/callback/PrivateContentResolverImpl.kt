package com.wavesenterprise.we.tx.observer.core.spring.method.callback

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.node.client.blocking.privacy.PrivacyService
import com.wavesenterprise.sdk.node.domain.privacy.PolicyItemInfoResponse
import com.wavesenterprise.sdk.node.domain.privacy.PolicyItemRequest
import com.wavesenterprise.sdk.node.domain.tx.PolicyDataHashTx
import com.wavesenterprise.sdk.node.exception.NodeBadRequestException
import com.wavesenterprise.we.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.we.tx.observer.api.privacy.PrivateDataEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.Type

class PrivateContentResolverImpl(
    private val nodeBlockingServiceFactory: NodeBlockingServiceFactory,
    private val objectMapper: ObjectMapper,
) : AbstractPrivateContentResolver(
    nodeBlockingServiceFactory = nodeBlockingServiceFactory,
    objectMapper = objectMapper,
) {

    private val privacyService: PrivacyService = nodeBlockingServiceFactory.privacyService()

    val logger: Logger = LoggerFactory.getLogger(PrivateContentResolver::class.java)

    @Suppress("UNCHECKED_CAST")
    override fun <T> decode(tx: PolicyDataHashTx, type: Type): PrivateDataEvent<T> {
        val policyTx = getPolicyTx(tx = tx)
        return PrivateDataEvent(
            payload = mapPayload(bytes = getPrivacyData(tx), type = type) as T,
            meta = mapMeta(privacyInfo = getPrivacyInfo(tx)),
            policyName = policyTx.policyName.value,
            createPolicyTx = policyTx,
            policyDataHashTx = tx,
        )
    }

    override fun decode(tx: PolicyDataHashTx): PrivateDataEvent<Nothing> {
        val policyTx = getPolicyTx(tx = tx)
        return PrivateDataEvent(
            meta = mapMeta(privacyInfo = getPrivacyInfo(tx)),
            policyName = policyTx.policyName.value,
            createPolicyTx = policyTx,
            policyDataHashTx = tx,
        )
    }

    override fun getMeta(tx: PolicyDataHashTx): JsonNode? =
        mapMeta(getPrivacyInfo(tx))

    override fun isAvailable(tx: PolicyDataHashTx): Boolean =
        try {
            privacyService.info(PolicyItemRequest(policyId = tx.policyId, dataHash = tx.dataHash)).isPresent
        } catch (ex: NodeBadRequestException) {
            logger.debug("Got exception while trying to resolve privacy availability. Returning false", ex)
            false
        }

    private fun mapMeta(privacyInfo: PolicyItemInfoResponse): JsonNode? =
        try {
            objectMapper.readTree(privacyInfo.info.comment.value)
        } catch (ex: JsonParseException) {
            null
        }
}
