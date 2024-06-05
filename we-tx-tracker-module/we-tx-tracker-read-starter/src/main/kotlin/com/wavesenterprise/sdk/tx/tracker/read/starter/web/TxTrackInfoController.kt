package com.wavesenterprise.sdk.tx.tracker.read.starter.web

import com.wavesenterprise.sdk.tx.tracker.api.TxTrackInfoService
import com.wavesenterprise.sdk.tx.tracker.read.starter.web.dto.TxTrackInfoApiDto
import com.wavesenterprise.sdk.tx.tracker.read.starter.web.dto.TxTrackInfoListRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tx-track-info")
class TxTrackInfoController(
    val txTrackInfoService: TxTrackInfoService,
) {

    @GetMapping("/{id}")
    fun getById(@PathVariable id: String): TxTrackInfoApiDto =
        txTrackInfoService.getById(id).toApiDto()

    @GetMapping
    fun list(filter: TxTrackInfoListRequest): List<TxTrackInfoApiDto> =
        txTrackInfoService.list(filter.toSpecification())
            .map { it.toApiDto() }
}
