package com.wavesenterprise.we.tx.tracker.read.starter.web.service

import com.wavesenterprise.we.tx.tracker.api.TxTrackInfoService
import com.wavesenterprise.we.tx.tracker.domain.TxTrackInfo
import com.wavesenterprise.we.tx.tracker.read.starter.TxTrackerReadJpaRepository
import com.wavesenterprise.we.tx.tracker.read.starter.web.service.exception.NoTrackInfoFoundException
import org.springframework.data.jpa.domain.Specification

open class TxTrackInfoServiceImpl(
    private val txTrackerReadJpaRepository: TxTrackerReadJpaRepository,
) : TxTrackInfoService {

    override fun getById(id: String): TxTrackInfo =
        txTrackerReadJpaRepository.findById(id)
            ?: throw NoTrackInfoFoundException(id)

    override fun list(spec: Specification<TxTrackInfo>): List<TxTrackInfo> =
        txTrackerReadJpaRepository.findAll(spec)
}
