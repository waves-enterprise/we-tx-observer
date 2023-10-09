package com.wavesenterprise.we.tx.observer.api.privacy

import com.fasterxml.jackson.databind.JsonNode
import com.wavesenterprise.sdk.node.domain.tx.CreatePolicyTx
import com.wavesenterprise.sdk.node.domain.tx.PolicyDataHashTx
import java.lang.reflect.Type

/**
 * Resolver for handling private data from PolicyDataHashTx (114)
 */
interface PrivateContentResolver {

    /**
     * Map tx info to parametrized PrivateDataEvent
     * @param tx PolicyDataHashTx(114)
     * @param type type of payload
     * @see PrivateDataEvent
     * @return parametrized PrivateDataEvent<T>
     */
    fun <T> decode(tx: PolicyDataHashTx, type: Type): PrivateDataEvent<T>

    /**
     * Map tx info to PrivateDataEvent without payload
     * @param tx PolicyDataHashTx(114)
     * @see PrivateDataEvent
     * @return PrivateDataEvent<Nothing>
     */
    fun decode(tx: PolicyDataHashTx): PrivateDataEvent<Nothing>

    /**
     * Map meta data to JsonNode (returns null if comment not in json format)
     * @param tx
     * @return nullable JsonNode value
     */
    fun getMeta(tx: PolicyDataHashTx): JsonNode?

    /**
     * Get CreatePolicyTx(112) by PolicyDataHashTx(114)
     * @param tx PolicyDataHashTx(114)
     * @return CreatePolicyTx(112)
     * @see CreatePolicyTx
     */
    fun getPolicyTx(tx: PolicyDataHashTx): CreatePolicyTx

    /**
     * Get policy name by PolicyDataHashTx(114)
     * @param tx PolicyDataHashTx(114)
     * @return policy name string
     */
    fun getPolicyName(tx: PolicyDataHashTx): String = getPolicyTx(tx).policyName.value

    /**
     * Check policy item info existence for by PolicyDataHashTx(114)
     * @param tx PolicyDataHashTx(114)
     * @return boolean
     */
    fun isAvailable(tx: PolicyDataHashTx): Boolean
}
