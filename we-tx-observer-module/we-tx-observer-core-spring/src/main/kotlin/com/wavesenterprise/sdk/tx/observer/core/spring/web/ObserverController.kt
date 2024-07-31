package com.wavesenterprise.sdk.tx.observer.core.spring.web

import com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo.BlockHistoryService
import com.wavesenterprise.sdk.tx.observer.core.spring.web.dto.BlockHistoryApiDto
import com.wavesenterprise.sdk.tx.observer.core.spring.web.dto.ObserverStatusApiDto
import com.wavesenterprise.sdk.tx.observer.core.spring.web.dto.RollbackInfoApiDto
import com.wavesenterprise.sdk.tx.observer.core.spring.web.service.RollbackInfoService
import com.wavesenterprise.sdk.tx.observer.core.spring.web.service.TxQueueService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/observer")
class ObserverController(
    private val txQueueService: TxQueueService,
    private val rollbackInfoService: RollbackInfoService,
    private val blockHistoryService: BlockHistoryService,
) {
    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun reset(desiredHeight: Long) {
        txQueueService.resetToHeightAsynchronously(desiredHeight)
    }

    @GetMapping("/status")
    fun status(): ObserverStatusApiDto =
        ObserverStatusApiDto(
            totalRollbackCount = rollbackInfoService.count(),
        )

    @GetMapping("/rollback")
    fun rollbackList(pageable: Pageable): Page<RollbackInfoApiDto> =
        rollbackInfoService.list(pageable).map { it.toApiDto() }

    @GetMapping("/block")
    fun blockHistory(pageable: Pageable): Page<BlockHistoryApiDto> =
        blockHistoryService.list(pageable).map { it.toApiDto() }
}
