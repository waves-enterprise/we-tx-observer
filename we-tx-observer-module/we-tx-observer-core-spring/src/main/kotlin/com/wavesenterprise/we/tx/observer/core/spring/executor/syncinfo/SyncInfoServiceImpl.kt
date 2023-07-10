package com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo

import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.BlockSearchResult.Found
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.BlockSearchResult.NotFound
import com.wavesenterprise.we.tx.observer.core.spring.metrics.MetricsContainer
import com.wavesenterprise.we.tx.observer.domain.BlockHeightInfo
import com.wavesenterprise.we.tx.observer.jpa.repository.BlockHeightJpaRepository
import org.slf4j.debug
import org.slf4j.info
import org.slf4j.lazyLogger
import org.slf4j.warn
import java.lang.Long.max

class SyncInfoServiceImpl(
    private val blockHeightJpaRepository: BlockHeightJpaRepository,
    private val blockHistoryService: BlockHistoryService,
    private val blocksService: BlocksService,
    private val nodeAliasForHeight: String,
    private val syncHistory: SyncHistoryProperties,
    private val autoResetHeight: Boolean,
    private val forkNotResolvedHeightDrop: Long,
    private val nodeHeightMetric: MetricsContainer,
    private val observerHeightMetric: MetricsContainer,
) : SyncInfoService {
    data class SyncHistoryProperties(
        val enabled: Boolean,
        val fromHeight: Long
    )

    private val log by lazyLogger(SyncInfoServiceImpl::class)
    private val blockInfoSingleRecord: BlockHeightInfo
        get() = blockHeightJpaRepository.findAll().firstOrNull()
            ?: blockHeightJpaRepository.save(
                BlockHeightInfo(
                    currentHeight = when {
                        syncHistory.enabled -> syncHistory.fromHeight
                        else -> blocksService.blockHeight().value
                    },
                    nodeAlias = nodeAliasForHeight
                )
            )

    private val observerHeight: Long
        get() = blockInfoSingleRecord.currentHeight

    override fun observerHeight(): Long =
        observerHeight

    override fun syncInfo(): SyncInfo {
        val nodeHeight = blocksService.blockHeight()
        val blockInfo = blockInfoSingleRecord.updated(nodeHeight)
        log.debug {
            "Current node block height = $nodeHeight; current block height in repo = ${blockInfo.currentHeight}"
        }
        return syncInfo(
            blockHeightInfo = blockInfo,
            nodeHeight = nodeHeight,
        )
    }

    private fun BlockHeightInfo.updated(nodeHeight: Height): BlockHeightInfo {
        fun BlockHeightInfo.resetToNodeHeightIfNecessary(): Boolean =
            when {
                autoResetHeight && currentHeight > nodeHeight.value -> {
                    log.warn {
                        "Observer height ($currentHeight) is greater than node height." +
                            " Setting height to nodeHeight: ${nodeHeight.value}"
                    }
                    currentHeight = nodeHeight.value
                    prevBlockSignature = null
                    true
                }
                else -> false
            }

        fun BlockHeightInfo.fillPrevBlockSignature(): Boolean =
            when {
                currentHeight == FIRST_BLOCK_HEIGHT || prevBlockSignature != null -> false
                else -> {
                    prevBlockSignature = prevBlockSignatureFromNode(currentHeight).asBase58String()
                    true
                }
            }

        val heightUpdated = resetToNodeHeightIfNecessary()
        val prevBlockSignatureUpdated = fillPrevBlockSignature()
        val updated = heightUpdated || prevBlockSignatureUpdated
        return if (updated)
            blockHeightJpaRepository.save(this)
        else
            this
    }

    override fun syncInfoOnFork(): SyncInfo =
        syncInfo(
            blockHeightInfo = when (
                val blockSearchResult =
                    blockHistoryService.latestCommonBlockWithNode(Height.fromLong(observerHeight))
            ) {
                is Found -> {
                    log.info {
                        "Found latest common block with node" +
                            " at height ${blockSearchResult.height} with signature ${blockSearchResult.signature}"
                    }
                    setBlockInfo(
                        height = blockSearchResult.height.value + 1,
                        prevBlockSignature = blockSearchResult.signature.asBase58String()
                    )
                }
                is NotFound -> {
                    val height: Long =
                        max(blockSearchResult.lastCheckedHeight.value - forkNotResolvedHeightDrop, FIRST_BLOCK_HEIGHT)
                    log.info {
                        "Latest common block with node not found" +
                            ", last checked height ${blockSearchResult.lastCheckedHeight}" +
                            ", setting block height to $height"
                    }
                    setBlockInfo(
                        height = height,
                        prevBlockSignature = prevBlockSignature(height)?.asBase58String()
                    )
                }
            },
            nodeHeight = blocksService.blockHeight(),
        )

    private fun syncInfo(
        blockHeightInfo: BlockHeightInfo,
        nodeHeight: Height
    ): SyncInfo =
        blockHeightInfo.toSyncInfo(nodeHeight).also { syncInfo ->
            blockHistoryService.clean(syncInfo.observerHeight.value)
            updateMetrics(syncInfo)
        }

    private fun updateMetrics(syncInfo: SyncInfo) = with(syncInfo) {
        updateMetrics(
            nodeHeight = nodeHeight,
            observerHeight = observerHeight,
        )
    }

    private fun updateMetrics(
        nodeHeight: Height,
        observerHeight: Height
    ) {
        nodeHeightMetric.metricValue = nodeHeight.value
        observerHeightMetric.metricValue = observerHeight.value
    }

    private fun prevBlockSignature(height: Long) = when (height) {
        FIRST_BLOCK_HEIGHT -> null
        else -> prevBlockSignatureFromNode(height)
    }

    private fun prevBlockSignatureFromNode(height: Long) =
        blocksService.blockAtHeight(height - 1).signature

    private fun setBlockInfo(height: Long, prevBlockSignature: String?): BlockHeightInfo =
        blockHeightJpaRepository.save(
            blockInfoSingleRecord.apply {
                this.currentHeight = height
                this.prevBlockSignature = prevBlockSignature
            }
        )

    override fun syncedTo(height: Long, prevBlockSignature: String?, syncedBlocks: List<SyncedBlockInfo>) {
        doSyncedTo(
            height = height,
            prevBlockSignature = prevBlockSignature,
            syncedBlocks = syncedBlocks,
        )
    }

    override fun syncedTo(
        height: Long,
        prevBlockSignature: String?,
        expectedCurrentHeight: Long,
        syncedBlocks: List<SyncedBlockInfo>
    ) {
        doSyncedTo(
            height = height,
            prevBlockSignature = prevBlockSignature,
            expectedCurrentHeight = expectedCurrentHeight,
            syncedBlocks = syncedBlocks,
        )
    }

    private fun doSyncedTo(
        height: Long,
        prevBlockSignature: String?,
        expectedCurrentHeight: Long? = null,
        syncedBlocks: List<SyncedBlockInfo>
    ) {
        checkHeight(height)
        log.debug {
            "Syncing [height = '$height', prevBlockSignature = '$prevBlockSignature'" +
                ", expectedCurrentHeight = '$expectedCurrentHeight']"
        }
        when {
            expectedCurrentHeight != null -> {
                blockHeightJpaRepository.update(
                    nodeAlias = blockInfoSingleRecord.nodeAlias,
                    currentHeight = height,
                    prevBlockSignature = prevBlockSignature,
                    expectedCurrentHeight = expectedCurrentHeight
                ).also { rowsUpdated ->
                    if (rowsUpdated == 0) throw ExpectedHeightMismatchException(expectedCurrentHeight)
                }
            }
            else -> setBlockInfo(height, prevBlockSignature)
        }
        blockHistoryService.clean(height)
        blockHistoryService.saveBlocks(syncedBlocks)
        updateMetrics(
            nodeHeight = blocksService.blockHeight(),
            observerHeight = Height.fromLong(height),
        )
    }

    override fun resetTo(height: Long, prevBlockSignature: String?) {
        checkHeight(height)
        log.info { "Resetting [height = '$height', prevBlockSignature = '$prevBlockSignature']" }
        setBlockInfo(height, prevBlockSignature)
        blockHistoryService.clean(height)
        updateMetrics(
            nodeHeight = blocksService.blockHeight(),
            observerHeight = Height.fromLong(height),
        )
    }

    private class SyncInfoAdapter(
        private val blockHeightInfo: BlockHeightInfo,
        override val nodeHeight: Height
    ) : SyncInfo {
        override val observerHeight: Height
            get() = Height.fromLong(blockHeightInfo.currentHeight)
        override val prevBlockSignature: Signature?
            get() = blockHeightInfo.prevBlockSignature?.let { Signature.fromBase58(it) }
    }

    companion object {
        private fun BlockHeightInfo.toSyncInfo(nodeHeight: Height): SyncInfo =
            SyncInfoAdapter(
                blockHeightInfo = this,
                nodeHeight = nodeHeight,
            )

        private fun checkHeight(height: Long) {
            if (height < FIRST_BLOCK_HEIGHT) error("Height can be less than $FIRST_BLOCK_HEIGHT")
        }
    }
}
