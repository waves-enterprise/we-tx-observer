package com.wavesenterprise.sdk.tx.observer.core.spring.web

import com.wavesenterprise.sdk.node.domain.TxId.Companion.base58TxId
import com.wavesenterprise.sdk.tx.observer.core.spring.web.dto.EnqueuedTxApiDto
import com.wavesenterprise.sdk.tx.observer.core.spring.web.dto.EnqueuedTxSearchRequest
import com.wavesenterprise.sdk.tx.observer.core.spring.web.dto.PatchTxApiDto
import com.wavesenterprise.sdk.tx.observer.core.spring.web.dto.QueueStatusApiDto
import com.wavesenterprise.sdk.tx.observer.core.spring.web.service.TxQueueService
import com.wavesenterprise.sdk.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/observer/queue")
class EnqueuedTxController(
    val enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
    val txQueueService: TxQueueService,
) {
    @GetMapping
    fun getPagedEnqueuedTx(filter: EnqueuedTxSearchRequest, pageRequest: Pageable): Page<EnqueuedTxApiDto> =
        enqueuedTxJpaRepository.findAll(filter.toSpecification(), pageRequest).map { it.toApiDto() }

    @GetMapping("/{txId}")
    fun getEnqueuedTx(@PathVariable txId: String) =
        txQueueService.getTxById(txId.base58TxId).toApiDto()

    @GetMapping("/status")
    fun status(): QueueStatusApiDto = txQueueService.getQueueStatus()

    @PostMapping("/postponeErrors")
    fun postponeErrors() = txQueueService.postponeErrors()

    @PutMapping("/{txId}")
    fun addTxToQueueById(@PathVariable txId: String): EnqueuedTxApiDto =
        txQueueService.addTxToQueueById(txId.base58TxId).toApiDto()

    @PatchMapping("/{txId}")
    fun patchTxInQueue(
        @PathVariable txId: String,
        @RequestBody patchTxApiDto: PatchTxApiDto,
    ): EnqueuedTxApiDto = txQueueService.changeTxStatusInQueue(txId, patchTxApiDto).toApiDto()

    @DeleteMapping("/{txId}")
    fun deleteTxFromQueue(@PathVariable txId: String) {
        txQueueService.deleteTxFromQueue(txId)
    }

    @DeleteMapping("/cleanForked")
    fun deleteCancelledForked() =
        mapOf("deletedCount" to txQueueService.deleteForked())
}
