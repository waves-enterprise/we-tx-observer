package com.wavesenterprise.we.tx.observer.api.privacy

import com.fasterxml.jackson.databind.JsonNode
import com.wavesenterprise.sdk.node.domain.tx.CreatePolicyTx
import com.wavesenterprise.sdk.node.domain.tx.PolicyDataHashTx
import com.wavesenterprise.we.tx.observer.api.NoPayloadException

data class PrivateDataEvent<T> private constructor(
    private val payloadInitializer: () -> T,
    val meta: JsonNode?,
    val policyName: String,
    val createPolicyTx: CreatePolicyTx,
    val policyDataHashTx: PolicyDataHashTx,
) {
    constructor(
        meta: JsonNode?,
        policyName: String,
        createPolicyTx: CreatePolicyTx,
        policyDataHashTx: PolicyDataHashTx
    ) : this(
        payloadInitializer = { throw NoPayloadException() },
        meta = meta,
        policyName = policyName,
        createPolicyTx = createPolicyTx,
        policyDataHashTx = policyDataHashTx,
    )

    constructor(
        payload: T,
        meta: JsonNode?,
        policyName: String,
        createPolicyTx: CreatePolicyTx,
        policyDataHashTx: PolicyDataHashTx,
    ) : this(
        payloadInitializer = { payload },
        meta = meta,
        policyName = policyName,
        createPolicyTx = createPolicyTx,
        policyDataHashTx = policyDataHashTx,
    )

    val payload: T by lazy(payloadInitializer)
}
