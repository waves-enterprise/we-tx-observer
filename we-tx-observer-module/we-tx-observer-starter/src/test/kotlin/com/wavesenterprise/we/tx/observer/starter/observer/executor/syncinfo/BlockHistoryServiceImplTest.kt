package com.wavesenterprise.we.tx.observer.starter.observer.executor.syncinfo

import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.sdk.node.domain.Address
import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.blocks.BlockAtHeight
import com.wavesenterprise.sdk.node.domain.blocks.BlockHeaders
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.BlockHistoryService
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.BlockHistoryServiceImpl
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.BlockSearchResult
import com.wavesenterprise.we.tx.observer.domain.BlockHistory
import com.wavesenterprise.we.tx.observer.jpa.repository.BlockHistoryRepository
import com.wavesenterprise.we.tx.observer.starter.observer.util.ModelFactory.blockAtHeight
import com.wavesenterprise.we.tx.observer.starter.observer.util.ModelFactory.blockHistory
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.stream.Stream
import kotlin.math.max
import kotlin.random.Random

@ExtendWith(MockKExtension::class)
class BlockHistoryServiceImplTest {
    @RelaxedMockK
    private lateinit var blockHistoryRepository: BlockHistoryRepository

    @RelaxedMockK
    private lateinit var blocksService: BlocksService

    @Test
    fun `should return latest common block with node in first block window`() {
        checkCommonLatestBlock(
            observerHeight = 50,
            historyDepth = 30,
            blockHeightWindow = 4,
            expectedLatestCommonBlockHeight = 50
        )
    }

    @Test
    fun `should return latest common block with node in last block window`() {
        checkCommonLatestBlock(
            observerHeight = 200,
            historyDepth = 10,
            blockHeightWindow = 3,
            expectedLatestCommonBlockHeight = 190
        )
    }

    @Test
    fun `should return latest common block with node in block window in the middle`() {
        checkCommonLatestBlock(
            observerHeight = 150,
            historyDepth = 20,
            blockHeightWindow = 5,
            expectedLatestCommonBlockHeight = 140
        )
    }

    @Test
    fun `should access blocks by ranges from the most recent block`() {
        val blockHeightWindow = 13L
        val historyDepth = 50
        val observerHeight = 80L
        val lastBlockHeight = observerHeight - historyDepth
        every { blockHistoryRepository.findAllByHeightBetweenOrderByHeightDesc(any(), any()) } answers {
            Stream.builder<BlockHistory>().apply {
                add(
                    blockHistory(
                        height = Random.nextLong(lastBlockHeight, observerHeight)
                    )
                )
            }.build()
        }
        val blockSeqRanges = sequence {
            var upperBound = observerHeight
            do {
                val lowerBound = max(upperBound - blockHeightWindow + 1, lastBlockHeight)
                yield(lowerBound to upperBound)
                upperBound = lowerBound - 1
            } while (upperBound >= lastBlockHeight)
        }

        val blockHistoryService: BlockHistoryService = BlockHistoryServiceImpl(
            blockHistoryRepository = blockHistoryRepository,
            historyDepth = historyDepth,
            blocksService = blocksService,
            blockWindowSize = blockHeightWindow
        )

        val blockSearchResult = blockHistoryService.latestCommonBlockWithNode(Height(observerHeight))

        assertThat(blockSearchResult).isEqualTo(BlockSearchResult.NotFound(lastCheckedHeight = Height(lastBlockHeight)))
        verifySequence {
            blockHistoryRepository.findAllByHeightBetweenOrderByHeightDesc(
                fromHeight = lastBlockHeight,
                toHeight = observerHeight
            )
            for ((fromHeight, toHeight) in blockSeqRanges) {
                blocksService.blockHeadersSequence(
                    fromHeight = fromHeight,
                    toHeight = toHeight
                )
            }
        }
        confirmVerified(blockHistoryRepository, blocksService)
    }

    private fun checkCommonLatestBlock(
        observerHeight: Long,
        historyDepth: Int,
        blockHeightWindow: Long,
        expectedLatestCommonBlockHeight: Long
    ) {
        val lastBlockHeight = observerHeight - historyDepth
        val weBlocksApi: BlocksService = BlocksServiceStub(
            (lastBlockHeight..observerHeight).map { height ->
                blockAtHeight(
                    signature = Signature.fromByteArray("signature_$height".toByteArray()),
                    height = Height(height)
                )
            }
        )

        val expectedLatestCommonBlock = weBlocksApi.blockAtHeight(expectedLatestCommonBlockHeight)
        every { blockHistoryRepository.findAllByHeightBetweenOrderByHeightDesc(lastBlockHeight, observerHeight) }
            .answers {
                (
                    (lastBlockHeight..expectedLatestCommonBlockHeight).map { height ->
                        with(weBlocksApi.blockAtHeight(height)) {
                            blockHistory(
                                signature = signature.asBase58String(),
                                height = height
                            )
                        }
                    } + (expectedLatestCommonBlockHeight + 1..observerHeight).map { height ->
                        blockHistory(
                            signature = "fork_signature_$height",
                            height = height
                        )
                    }
                    ).stream()
            }

        val blockHistoryService: BlockHistoryService = BlockHistoryServiceImpl(
            blockHistoryRepository = blockHistoryRepository,
            historyDepth = historyDepth,
            blocksService = weBlocksApi,
            blockWindowSize = blockHeightWindow
        )

        val blockSearchResult = blockHistoryService.latestCommonBlockWithNode(Height(observerHeight))

        assertThat(blockSearchResult).isEqualTo(
            BlockSearchResult.Found(
                signature = expectedLatestCommonBlock.signature,
                height = expectedLatestCommonBlock.height
            )
        )
    }

    private class BlocksServiceStub private constructor(
        private val blockByHeight: Map<Height, BlockAtHeight>,
        private val blockBySignature: Map<Signature, BlockAtHeight>,
        private val blockHeight: Height
    ) : BlocksService {
        constructor(blocks: Iterable<BlockAtHeight>) : this(
            blocks.associateBy { it.height },
            blocks.associateBy { it.signature },
            Height(blocks.maxOfOrNull { it.height.value } ?: 0)
        )

        override fun blockAtHeight(height: Long): BlockAtHeight = blockByHeight.getValue(Height(height))
        override fun blockHeadersSequence(fromHeight: Long, toHeight: Long): List<BlockHeaders> =
            blockSequence(fromHeight, toHeight).map { blocksAtHeightDto ->
                blocksAtHeightDto.toBlocksHeadersDto()
            }

        override fun blockHeight(): Height = blockHeight

        override fun blockHeightById(signature: Signature): Height =
            blockBySignature.getValue(signature).height

        override fun blockSequence(fromHeight: Long, toHeight: Long): List<BlockAtHeight> =
            (fromHeight..toHeight).map { height ->
                blockByHeight.getValue(Height(height))
            }

        override fun blockById(signature: Signature): BlockAtHeight {
            TODO("Not yet implemented")
        }

        override fun blockHeadersAtHeight(height: Long): BlockHeaders {
            TODO("Not yet implemented")
        }

        override fun blocksByAddress(address: Address, fromHeight: Long, toHeight: Long): List<BlockAtHeight> {
            TODO("Not yet implemented")
        }

        override fun blocksExtSequence(fromHeight: Long, toHeight: Long): List<BlockAtHeight> {
            TODO("Not yet implemented")
        }

        override fun childBlock(signature: Signature): BlockAtHeight {
            TODO("Not yet implemented")
        }

        override fun firstBlock(): BlockAtHeight {
            TODO("Not yet implemented")
        }

        override fun lastBlock(): BlockAtHeight {
            TODO("Not yet implemented")
        }

        override fun lastBlockHeader(): BlockHeaders {
            TODO("Not yet implemented")
        }

        companion object {
            private fun BlockAtHeight.toBlocksHeadersDto(): BlockHeaders =
                BlockHeaders(
                    reference = reference,
                    blockSize = blockSize,
                    features = features,
                    signature = signature,
                    generator = generator,
                    transactionCount = transactionCount,
                    version = version,
                    poaConsensus = poaConsensus,
                    posConsensus = posConsensus,
                    timestamp = timestamp,
                    height = height
                )
        }
    }
}
