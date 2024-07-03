package com.wavesenterprise.sdk.tx.observer.jpa.repository

import com.wavesenterprise.sdk.tx.observer.domain.BlockHeightInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BlockHeightJpaRepository : JpaRepository<BlockHeightInfo, String> {
    @Query(
        """
        update BlockHeightInfo
        set
            currentHeight = :currentHeight,
            prevBlockSignature = :prevBlockSignature
        where
            id = :id
            and currentHeight = :expectedCurrentHeight
    """,
    )
    @Modifying
    fun update(id: String, currentHeight: Long, prevBlockSignature: String?, expectedCurrentHeight: Long): Int
}
