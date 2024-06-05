package com.wavesenterprise.sdk.tx.observer.core.spring.executor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.wavesenterprise.sdk.node.client.http.tx.PolicyDataHashTxDto
import com.wavesenterprise.sdk.node.client.http.tx.PolicyDataHashTxDto.Companion.toDomain
import com.wavesenterprise.sdk.node.client.http.tx.TxDto
import com.wavesenterprise.sdk.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.sdk.tx.observer.core.spring.component.OffsetProvider
import com.wavesenterprise.sdk.tx.observer.core.spring.metrics.AddableLongMetricsContainer
import com.wavesenterprise.sdk.tx.observer.core.spring.partition.PartitionHandler
import com.wavesenterprise.sdk.tx.observer.core.spring.properties.PrivacyAvailabilityCheckConfig
import com.wavesenterprise.sdk.tx.observer.domain.EnqueuedTx
import com.wavesenterprise.sdk.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

open class PrivacyAvailabilityChecker(
    val enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
    val privateContentResolver: PrivateContentResolver,
    val partitionHandler: PartitionHandler,
    val offsetProvider: OffsetProvider,
    val objectMapper: ObjectMapper,
    val properties: PrivacyAvailabilityCheckConfig,
    val privacyThatBecameAvailableMetric: AddableLongMetricsContainer,
) {

    val logger: Logger = LoggerFactory.getLogger(PrivacyAvailabilityChecker::class.java)

    @Transactional
    open fun checkPrivacyAvailability(): Long {
        logger.trace("Start to check privacy availability")
        val candidateCount = enqueuedTxJpaRepository.countNotAvailablePolicyDataHashes()
        if (candidateCount == 0L) {
            return candidateCount
        }
        logger.debug("Checking privacy availability candidates. Total candidates $candidateCount")
        val oldCandidates = getOldCandidates(candidateCount)
        val recentCandidates = getRecentCandidates(candidateCount, oldCandidates)
        logger.debug(
            "Got ${oldCandidates.size} old candidates and ${recentCandidates.size} " +
                "recent candidates for privacy check"
        )
        sequenceOf(oldCandidates, recentCandidates)
            .flatten()
            .onEach {
                val policyDataHashTx = objectMapper.treeToValue<TxDto>(it.body) as PolicyDataHashTxDto
                try {
                    it.available = privateContentResolver.isAvailable(policyDataHashTx.toDomain())
                } catch (ex: Exception) {
                    logger.error(
                        "Error while resolving private content for 114 TX with ID = ${policyDataHashTx.id}", ex
                    )
                }
            }.filter {
                it.available
            }.onEach {
                partitionHandler.resumePartitionForTx(it.partition.id, it.id)
            }.toList().apply {
                privacyThatBecameAvailableMetric.add(this.size.toLong())
                logger.debug("${this.size.toLong()} privacy became available")
                enqueuedTxJpaRepository.saveAll(this)
            }
        return candidateCount
    }

    private fun getRecentCandidates(candidateCount: Long, oldCandidates: List<EnqueuedTx>): List<EnqueuedTx> {
        val offset = offsetProvider.provideOffset(
            candidateCount.toInt() - oldCandidates.size - properties.limitForRecent
        )
        logger.trace("Searching for recent candidates with offset = $offset and limit = ${properties.limitForRecent}")
        return enqueuedTxJpaRepository.findRecentCheckPrivacyAvailabilityCandidates(
            alreadySelectedIds = oldCandidates.map { it.id }.toSet(),
            offset = offset,
            limit = properties.limitForRecent,
        )
    }

    private fun getOldCandidates(candidateCount: Long): List<EnqueuedTx> {
        val offset = offsetProvider.provideOffset(candidateCount.toInt() - properties.limitForOld)
        logger.trace("Searching for old candidates with offset = $offset and limit = ${properties.limitForOld}")
        return enqueuedTxJpaRepository.findOldCheckPrivacyAvailabilityCandidates(
            offset = offset,
            limit = properties.limitForOld,
        )
    }
}
