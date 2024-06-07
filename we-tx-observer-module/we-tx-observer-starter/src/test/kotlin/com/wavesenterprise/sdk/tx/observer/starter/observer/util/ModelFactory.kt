package com.wavesenterprise.sdk.tx.observer.starter.observer.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.wavesenterprise.sdk.node.client.http.tx.TxDto
import com.wavesenterprise.sdk.node.domain.BlockVersion
import com.wavesenterprise.sdk.node.domain.Feature
import com.wavesenterprise.sdk.node.domain.Fee
import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.Timestamp.Companion.toDateTimeFromUTCBlockChain
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.blocks.BlockAtHeight
import com.wavesenterprise.sdk.node.domain.blocks.BlockHeaders
import com.wavesenterprise.sdk.node.domain.blocks.PoaConsensus
import com.wavesenterprise.sdk.node.domain.blocks.PosConsensus
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo.FIRST_BLOCK_HEIGHT
import com.wavesenterprise.sdk.tx.observer.domain.BlockHistory
import com.wavesenterprise.sdk.tx.observer.domain.EnqueuedTx
import com.wavesenterprise.sdk.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.sdk.tx.observer.domain.TxQueuePartition
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.sampleCallContractTx
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.sampleCreateContractTx
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.sampleExecutedContractTx
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

object ModelFactory {

    fun enqueuedTx(
        tx: TxDto,
        positionInBlock: Int = 1,
        blockHeight: Long = 1,
        partition: TxQueuePartition,
        status: EnqueuedTxStatus = EnqueuedTxStatus.NEW,
        available: Boolean = true
    ) = EnqueuedTx(
        id = tx.id,
        body = jacksonObjectMapper().valueToTree(tx),
        txType = tx.type,
        status = status,
        blockHeight = blockHeight,
        positionInBlock = positionInBlock,
        txTimestamp = tx.timestamp.toDateTimeFromUTCBlockChain(),
        partition = partition,
        available = available,
        positionInAtomic = 0,
        atomicTxId = null
    )

    fun blockAtHeight(
        reference: String = "reference",
        blockSize: Long = 0L,
        features: List<Feature> = emptyList(),
        signature: Signature = Signature.fromBase58("signature"),
        fee: Fee = Fee.fromInt(0),
        generator: String = "generator",
        transactionCount: Long = 0L,
        transactions: List<Tx> = emptyList(),
        version: BlockVersion = BlockVersion.fromInt(1),
        poaConsensus: PoaConsensus? = null,
        posConsensus: PosConsensus? = null,
        timestamp: Long = 0,
        height: Height = Height.fromLong(1),
    ): BlockAtHeight = BlockAtHeight(
        reference = reference,
        blockSize = blockSize,
        features = features,
        signature = signature,
        fee = fee,
        generator = generator,
        transactionCount = transactionCount,
        transactions = transactions,
        version = version,
        poaConsensus = poaConsensus,
        posConsensus = posConsensus,
        timestamp = timestamp,
        height = height
    )

    fun blockHeaders(
        reference: String = "reference",
        blockSize: Long = 0L,
        features: List<Feature> = emptyList(),
        signature: Signature = Signature.fromBase58("signature"),
        generator: String = "generator",
        transactionCount: Long = 0L,
        version: BlockVersion = BlockVersion.fromInt(1),
        poaConsensus: PoaConsensus? = null,
        posConsensus: PosConsensus? = null,
        timestamp: Long = 0,
        height: Height = Height.fromLong(1),
    ) = BlockHeaders(
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

    fun blockHistory(
        signature: String = UUID.randomUUID().toString(),
        height: Long = FIRST_BLOCK_HEIGHT,
        timestamp: OffsetDateTime = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)
    ): BlockHistory =
        BlockHistory(
            signature = signature,
            height = height,
            timestamp = timestamp
        )

    val txList: List<Tx> = listOf(
        sampleCreateContractTx.copy(id = TxId.fromByteArray("id1".toByteArray())),
        sampleCreateContractTx.copy(id = TxId.fromByteArray("id2".toByteArray())),
        sampleCreateContractTx.copy(id = TxId.fromByteArray("id3".toByteArray())),
        sampleCallContractTx.copy(id = TxId.fromByteArray("id11".toByteArray())),
        sampleExecutedContractTx.copy(
            id = TxId.fromByteArray("id1000".toByteArray()),
            tx = sampleCallContractTx.copy(id = TxId.fromByteArray("id1001".toByteArray())),
        )
    )
}
