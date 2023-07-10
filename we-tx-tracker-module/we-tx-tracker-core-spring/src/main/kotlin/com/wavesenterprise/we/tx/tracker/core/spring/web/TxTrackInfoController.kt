package com.wavesenterprise.we.tx.tracker.core.spring.web

import com.wavesenterprise.we.tx.tracker.core.spring.web.dto.TxTrackInfoApiDto
import com.wavesenterprise.we.tx.tracker.core.spring.web.dto.TxTrackInfoListRequest
import com.wavesenterprise.we.tx.tracker.core.spring.web.service.TxTrackInfoService
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
