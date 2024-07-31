package com.wavesenterprise.sdk.tx.observer.core.spring.web

import com.wavesenterprise.sdk.tx.observer.core.spring.web.dto.TxQueuePartitionSearchRequest
import com.wavesenterprise.sdk.tx.observer.core.spring.web.dto.TxQueuePartitionStatusApiDto
import com.wavesenterprise.sdk.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/observer/partitions")
class TxQueuePartitionController(
    val txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository,
) {
    @GetMapping
    fun getPagedEnqueuedTx(filter: TxQueuePartitionSearchRequest, pageRequest: Pageable) =
        txQueuePartitionJpaRepository.findAll(filter.toSpecification(), pageRequest).map { it.toApiDto() }

    @GetMapping("/{partitionId}")
    fun getPartitionById(@PathVariable partitionId: String) = txQueuePartitionJpaRepository.findByIdOrNull(partitionId)
        ?: throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Partition with ID = $partitionId not found in tx-observer queue",
        )

    @GetMapping("/status")
    fun status() = TxQueuePartitionStatusApiDto(
        txQueuePartitionJpaRepository.countErrorPartitions(),
        txQueuePartitionJpaRepository.count(),
    )
}
