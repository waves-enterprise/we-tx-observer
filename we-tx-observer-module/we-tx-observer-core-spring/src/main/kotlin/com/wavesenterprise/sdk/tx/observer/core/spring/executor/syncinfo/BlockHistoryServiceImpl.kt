package com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo

import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.Timestamp.Companion.toDateTimeFromUTCBlockChain
import com.wavesenterprise.sdk.tx.observer.domain.BlockHistory
import com.wavesenterprise.sdk.tx.observer.jpa.repository.BlockHistoryRepository
import org.slf4j.debug
import org.slf4j.lazyLogger
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional
import java.util.stream.Collectors.groupingBy
import java.util.stream.Collectors.mapping
import java.util.stream.Collectors.toSet
import kotlin.math.max

open class BlockHistoryServiceImpl(
    private val blockHistoryRepository: BlockHistoryRepository,
    private val historyDepth: Int,
    private val blocksService: BlocksService,
    private val blockWindowSize: Long,
) : BlockHistoryService {
    private val log by lazyLogger(BlockHistoryServiceImpl::class)

    init {
        check(historyDepth >= 0) { "historyDepth should be 0 or greater" }
        check(blockWindowSize >= 1) { "blockWindowSize should be 1 or greater" }
    }

    override fun saveBlocks(blocks: List<SyncedBlockInfo>) {
        val relevantSyncedBlockInfos =
            if (blocks.size <= historyDepth)
                blocks
            else
                blocks.sortedBy { it.height.value }.takeLast(historyDepth)
        blockHistoryRepository.saveAll(
            relevantSyncedBlockInfos.map { syncedBlockInfo ->
                with(syncedBlockInfo) {
                    BlockHistory(
                        height = height.value,
                        signature = signature.asBase58String(),
                        timestamp = timestamp.toDateTimeFromUTCBlockChain(),
                    )
                }
            }
        )
    }

    override fun list(pageable: Pageable): Page<BlockHistory> =
        blockHistoryRepository.findAll(
            { _, _, _ -> null },
            pageable
        )

    override fun clean(observerHeight: Long) {
        val cleanBeforeHeight = observerHeight - historyDepth
        clean(cleanBeforeHeight, observerHeight)
    }

    private fun clean(cleanBeforeHeight: Long, cleanAfterHeight: Long) {
        log.debug { "Cleaning block history before $cleanBeforeHeight and after $cleanAfterHeight" }
        blockHistoryRepository.deleteAllByHeightBefore(cleanBeforeHeight)
        blockHistoryRepository.deleteAllByHeightAfter(cleanAfterHeight)
    }

    @Transactional(readOnly = true)
    override fun latestCommonBlockWithNode(observerHeight: Height): BlockSearchResult {
        val heights = (observerHeight.value downTo observerHeight.value - historyDepth)
        val blockSignaturesFromRepoByHeight: Map<Long, Set<String>> =
            blockHistoryRepository
                .findAllByHeightBetweenOrderByHeightDesc(
                    fromHeight = heights.last,
                    toHeight = heights.first,
                ).use { blockHistoryFromRepo ->
                    blockHistoryFromRepo.collect(
                        groupingBy(
                            BlockHistory::height,
                            mapping(
                                BlockHistory::signature,
                                toSet<String>()
                            )
                        )
                    )
                }
        return if (blockSignaturesFromRepoByHeight.isEmpty()) BlockSearchResult.NotFound(lastCheckedHeight = observerHeight)
        else {
            val blockSignaturesFromNode = BlockSignaturesFromNode(
                fromHeight = heights.last,
                toHeight = heights.first,
            )
            val firstCommonBlockInfo = heights
                .mapNotNull { height ->
                    blockSignaturesFromNode[height]?.let { signature ->
                        BlockInfo(signature, height)
                    }
                }
                .firstOrNull { (signature, height) ->
                    blockSignaturesFromRepoByHeight[height]?.let { repoSignatures ->
                        signature in repoSignatures
                    } ?: false
                }
            if (firstCommonBlockInfo == null) BlockSearchResult.NotFound(Height.fromLong(heights.last))
            else {
                val (signature, height) = firstCommonBlockInfo
                BlockSearchResult.Found(
                    signature = Signature.fromBase58(signature),
                    height = Height.fromLong(height),
                )
            }
        }
    }

    private data class BlockInfo(
        val signature: String,
        val height: Long,
    )

    inner class BlockSignaturesFromNode(
        fromHeight: Long,
        toHeight: Long,
    ) {
        private val signatureChunkByHeight: Map<Long, Lazy<Map<Long, String>>>

        init {
            val blockSeqRanges = sequence {
                var upperBound = toHeight
                do {
                    val lowerBound = max(upperBound - blockWindowSize + 1, fromHeight)
                    yield(lowerBound..upperBound)
                    upperBound = lowerBound - 1
                } while (upperBound >= fromHeight)
            }
            val signaturesByRange = blockSeqRanges.associateWith { range ->
                lazy {
                    blocksService
                        .blockHeadersSequence(
                            fromHeight = range.first,
                            toHeight = range.last,
                        )
                        .associateBy { dto -> dto.height.value }
                        .mapValues { (_, dto) -> dto.signature.asBase58String() }
                        .also { log.debug { "Signatures by height: $it" } }
                }
            }
            signatureChunkByHeight = (fromHeight..toHeight).associateWith { height ->
                signaturesByRange.entries
                    .firstOrNull { (range, _) ->
                        height in range
                    }
                    ?.value
                    ?: lazy { emptyMap() }
            }
        }

        private fun getSignatureFromChunk(height: Long) =
            signatureChunkByHeight[height]?.value?.get(height)

        operator fun get(height: Long): String? =
            getSignatureFromChunk(height)
    }
}
