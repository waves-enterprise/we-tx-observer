package com.wavesenterprise.we.tx.observer.jpa.repository

import com.wavesenterprise.we.tx.observer.domain.BlockHeightInfo
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
            nodeAlias = :nodeAlias
            and currentHeight = :expectedCurrentHeight
    """
    )
    @Modifying
    fun update(nodeAlias: String?, currentHeight: Long, prevBlockSignature: String?, expectedCurrentHeight: Long): Int
}
