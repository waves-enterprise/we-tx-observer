package com.wavesenterprise.we.tx.tracker.core.spring.web.service

import com.wavesenterprise.we.tx.tracker.domain.TxTrackInfo
import org.springframework.data.jpa.domain.Specification

interface TxTrackInfoService {

    fun getById(id: String): TxTrackInfo

    fun list(spec: Specification<TxTrackInfo>): List<TxTrackInfo>
}
