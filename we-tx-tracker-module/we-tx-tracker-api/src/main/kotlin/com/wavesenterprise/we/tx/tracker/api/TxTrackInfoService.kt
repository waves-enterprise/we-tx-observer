package com.wavesenterprise.we.tx.tracker.api

import com.wavesenterprise.we.tx.tracker.domain.TxTrackInfo
import org.springframework.data.jpa.domain.Specification

/**
 * Service for getting actual information on tracking transactions.
 */
interface TxTrackInfoService {

    /**
     * Get TxTrackInfo by tx id
     * @param id tx id
     * @return TxTrackInfo
     */
    fun getById(id: String): TxTrackInfo

    /**
     * Get list of TxTrackInfo by specification of TxTrackInfo
     * @param spec parametrized by TxTrackInfo specification
     * @return list of TxTrackInfo
     */
    fun list(spec: Specification<TxTrackInfo>): List<TxTrackInfo>
}
