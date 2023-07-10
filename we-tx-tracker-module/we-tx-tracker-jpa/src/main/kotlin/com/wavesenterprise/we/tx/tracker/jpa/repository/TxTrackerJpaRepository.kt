package com.wavesenterprise.we.tx.tracker.jpa.repository

import com.fasterxml.jackson.databind.JsonNode
import com.wavesenterprise.we.tx.tracker.domain.TxTrackBusinessObjectInfo
import com.wavesenterprise.we.tx.tracker.domain.TxTrackInfo
import com.wavesenterprise.we.tx.tracker.domain.TxTrackStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TxTrackerJpaRepository : JpaRepository<TxTrackInfo, String> {

    fun existsByIdAndStatus(txId: String, txTrackerStatus: TxTrackStatus): Boolean
    fun findAllByStatus(txTrackerStatus: TxTrackStatus): List<TxTrackInfo>

    @Query(
        value = """SELECT tti.id FROM TxTrackInfo tti
        WHERE tti.status = :status
        AND tti.type IN :types
        ORDER BY tti.created """
    )
    fun findLastIdsByStatusAndTypeIn(
        @Param("status") txTrackerStatus: TxTrackStatus,
        @Param("types") types: List<Int>,
        pageable: Pageable,
    ): List<String>

    @Query(
        value = """SELECT tti.body FROM TxTrackInfo tti
        WHERE tti.status = :status
        ORDER BY tti.created """
    )
    fun findLastBodiesByStatus(
        @Param("status") txTrackerStatus: TxTrackStatus,
        pageable: Pageable,
    ): List<JsonNode?>

    fun findFirstByStatusNotAndBusinessObjectInfosContainsOrderByModifiedDesc(
        status: TxTrackStatus,
        businessObjectInfos: TxTrackBusinessObjectInfo,
    ): TxTrackInfo?
}
