package com.wavesenterprise.sdk.tx.tracker.starter

import com.wavesenterprise.sdk.node.client.blocking.contract.ContractService
import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.node.client.blocking.tx.TxService
import com.wavesenterprise.sdk.node.domain.Timestamp
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.contract.TxStatus
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.domain.tx.TxInfo
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.node.test.data.Util.Companion.randomBytesFromUUID
import com.wavesenterprise.sdk.tx.tracker.api.TxTracker
import com.wavesenterprise.sdk.tx.tracker.core.spring.component.ScheduledTxTracker
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackStatus
import com.wavesenterprise.sdk.tx.tracker.starter.properties.TxTrackerProperties
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageRequest
import java.time.Duration
import java.time.Instant
import java.util.Optional

@ExtendWith(MockKExtension::class)
internal class ScheduledTxTrackerTest {

    @RelaxedMockK
    lateinit var txTracker: TxTracker

    @RelaxedMockK
    lateinit var nodeBlockingServiceFactory: NodeBlockingServiceFactory

    @RelaxedMockK
    lateinit var txService: TxService

    @RelaxedMockK
    lateinit var contractService: ContractService

    @RelaxedMockK
    lateinit var txTrackerProperties: TxTrackerProperties

    private lateinit var scheduledTxTracker: ScheduledTxTracker

    private val txTimeout: Duration = Duration.ofHours(1)

    @BeforeEach
    fun setUp() {
        every { txTrackerProperties.trackedTxPageRequestLimit } returns 1000
        every { txTrackerProperties.timeout } returns txTimeout
        every { nodeBlockingServiceFactory.txService() } returns txService
        every { nodeBlockingServiceFactory.contractService() } returns contractService
        scheduledTxTracker = ScheduledTxTracker(
            nodeBlockingServiceFactory = nodeBlockingServiceFactory,
            txTracker = txTracker,
            txTrackerProperties = txTrackerProperties,
        )
    }

    @Test
    fun `should track contract tx status for pending not in unconfirmed list`() {
        val txId = mutableListOf<TxId>()
        val txIdToSetErrors = mutableListOf<TxId>()
        val txIdList = listOf(
            TxId(randomBytesFromUUID()),
            TxId(randomBytesFromUUID()),
            TxId(randomBytesFromUUID()),
            TxId(randomBytesFromUUID()),
        )
        val failedContractTxIds = txIdList.subList(0, 2)
        val txList = txIdList.map {
            TestDataFactory.createContractTx(
                id = it,
                timestamp = Timestamp(Instant.now().toEpochMilli()),
            )
        }.toList()
        val mockedUnconfirmedTxList = txList.subList(2, txIdList.size)
        val mockedErrorList = listOf(
            TestDataFactory.contractTxStatus(
                status = TxStatus.FAILURE,
            )
        )
        mockTrackedTxs(txList)
        every { contractService.getContractTxStatus(any()) } returns mockedErrorList
        every { txService.utxInfo() } returns mockedUnconfirmedTxList

        scheduledTxTracker.trackPendingTx()

        verify {
            txTracker.getTrackedTxsWithStatus(
                txTrackerStatus = TxTrackStatus.PENDING,
                pageRequest = PageRequest.of(0, 1000),
            )
        }

        verify { txService.utxInfo() }
        verify(exactly = failedContractTxIds.size) { contractService.getContractTxStatus(capture(txId)) }
        verify {
            txTracker.setContractTxError(
                txId = capture(txIdToSetErrors),
                contractTxStatusDtoList = mockedErrorList,
            )
        }
        verify { txService.txInfo(any()) }
        assertEquals(failedContractTxIds, txId)
        assertEquals(failedContractTxIds, txIdToSetErrors)
    }

    @Test
    fun `should set tx status to success when transactionInfo is present`() {
        val tx = TestDataFactory.createContractTx(
            id = TxId.fromBase58("C2HM9q3QzGSBydnCA4GMcf3cFnTaSuwaWXVtsCSTSmZW"),
            timestamp = Timestamp(Instant.now().minus(txTimeout + Duration.ofMinutes(1)).toEpochMilli())
        )
        mockTrackedTxs(listOf(tx))
        every { txService.txInfo(tx.id) } returns Optional.of(
            TxInfo(
                height = mockk(),
                tx = tx,
            )
        )
        scheduledTxTracker.trackPendingTx()
        verify { txService.utxInfo() }
        verify {
            txTracker.getTrackedTxsWithStatus(
                txTrackerStatus = TxTrackStatus.PENDING,
                pageRequest = PageRequest.of(0, 1000),
            )
        }

        verify { txService.txInfo(tx.id) }
        verify(exactly = 1) {
            txTracker.setTrackStatus(
                tx = tx,
                status = TxTrackStatus.SUCCESS,
            )
        }
    }

    @Test
    fun `should set tx status to failure when timeout reached and no transactionInfo`() {
        val tx = TestDataFactory.createContractTx(
            id = TxId.fromBase58("C2HM9q3QzGSBydnCA4GMcf3cFnTaSuwaWXVtsCSTSmZW"),
            timestamp = Timestamp(Instant.now().minus(txTimeout + Duration.ofMinutes(1)).toEpochMilli())
        )
        mockTrackedTxs(listOf(tx))

        every { txService.txInfo(tx.id) } returns Optional.empty()
        scheduledTxTracker.trackPendingTx()
        verify { txService.utxInfo() }
        verify {
            txTracker.getTrackedTxsWithStatus(
                txTrackerStatus = TxTrackStatus.PENDING,
                pageRequest = PageRequest.of(0, 1000),
            )
        }

        verify { txService.txInfo(tx.id) }
        verify(exactly = 1) {
            txTracker.setTrackStatus(
                tx = tx,
                status = TxTrackStatus.FAILURE,
            )
        }
    }

    @Test
    fun `should set tx status to success when timeout reached but transactionInfo is present`() {
        val tx = TestDataFactory.createContractTx(
            id = TxId.fromBase58("C2HM9q3QzGSBydnCA4GMcf3cFnTaSuwaWXVtsCSTSmZW"),
            timestamp = Timestamp(Instant.now().minus(txTimeout + Duration.ofMinutes(1)).toEpochMilli())
        )
        mockTrackedTxs(listOf(tx))

        every { txService.txInfo(tx.id) } returns Optional.of(
            TxInfo(
                height = mockk(),
                tx = tx,
            )
        )
        scheduledTxTracker.trackPendingTx()
        verify { txService.utxInfo() }
        verify {
            txTracker.getTrackedTxsWithStatus(
                txTrackerStatus = TxTrackStatus.PENDING,
                pageRequest = PageRequest.of(0, 1000),
            )
        }

        verify { txService.txInfo(tx.id) }
        verify(exactly = 1) {
            txTracker.setTrackStatus(
                tx = tx,
                status = TxTrackStatus.SUCCESS,
            )
        }
    }

    @Test
    fun `should not work if tracked tx list is empty`() {
        mockTrackedTxs(emptyList())

        scheduledTxTracker.trackPendingTx()

        verify(exactly = 1) {
            txTracker.getTrackedTxsWithStatus(
                txTrackerStatus = TxTrackStatus.PENDING,
                pageRequest = PageRequest.of(0, 1000),
            )
        }
    }

    @Test
    fun `should set status failure only for not contract tx when timeout reached`() {
        val tx = TestDataFactory.createPolicyTx(
            id = TxId.fromBase58("C2HM9q3QzGSBydnCA4GMcf3cFnTaSuwaWXVtsCSTSmZW"),
            timestamp = Timestamp(Instant.now().minus(txTimeout + Duration.ofMinutes(1)).toEpochMilli())
        )
        mockTrackedTxs(listOf(tx))
        every { txService.txInfo(tx.id) } returns Optional.empty()

        scheduledTxTracker.trackPendingTx()

        verify { txService.utxInfo() }
        verify {
            txTracker.getTrackedTxsWithStatus(
                txTrackerStatus = TxTrackStatus.PENDING,
                pageRequest = PageRequest.of(0, 1000),
            )
        }
        verify { txService.txInfo(tx.id) }
        verify(exactly = 1) {
            txTracker.setTrackStatus(
                tx = tx,
                status = TxTrackStatus.FAILURE,
            )
        }
    }

    @Test
    fun `should not set status to tx in UTX atomic`() {
        val tx = TestDataFactory.createPolicyTx(
            id = TxId.fromBase58("C2HM9q3QzGSBydnCA4GMcf3cFnTaSuwaWXVtsCSTSmZW"),
            timestamp = Timestamp(Instant.now().minus(txTimeout + Duration.ofMinutes(1)).toEpochMilli())
        )
        mockTrackedTxs(listOf(tx))

        val atomicTx = TestDataFactory.atomicTx(txs = listOf(tx), senderAddress = tx.senderAddress)
        every { txService.utxInfo() } returns listOf(atomicTx)

        scheduledTxTracker.trackPendingTx()

        verify { txService.utxInfo() }
        verify(exactly = 1) {
            txTracker.getTrackedTxsWithStatus(
                txTrackerStatus = TxTrackStatus.PENDING,
                pageRequest = PageRequest.of(0, 1000),
            )
        }
    }

    private fun mockTrackedTxs(txList: List<Tx>) {
        every {
            txTracker.getTrackedTxsWithStatus(
                TxTrackStatus.PENDING,
                PageRequest.of(0, 1000)
            )
        } returns txList
    }
}
