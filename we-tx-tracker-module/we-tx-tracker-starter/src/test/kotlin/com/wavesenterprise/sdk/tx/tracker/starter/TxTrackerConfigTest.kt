package com.wavesenterprise.sdk.tx.tracker.starter

import com.ninjasquad.springmockk.MockkBean
import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.tx.ExecutableTx
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.node.test.data.Util
import com.wavesenterprise.sdk.tx.observer.common.tx.subscriber.TxSubscriber
import com.wavesenterprise.sdk.tx.tracker.api.TxTrackInfoService
import com.wavesenterprise.sdk.tx.tracker.api.TxTracker
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackStatus
import com.wavesenterprise.sdk.tx.tracker.jpa.TxTrackerJpaAutoConfig
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [
        TxTrackerJpaAutoConfig::class,
        TxTrackerConfig::class,
    ],
    properties = [
        "tx-tracker.enabled = true",
        "debug=true"
    ]
)
internal class TxTrackerConfigTest {

    @MockkBean(relaxed = true)
    lateinit var txTracker: TxTracker

    @MockkBean(relaxed = true)
    lateinit var nodeBlockingServiceFactory: NodeBlockingServiceFactory

    @MockkBean(relaxed = true)
    lateinit var txTrackInfoService: TxTrackInfoService

    @Autowired
    lateinit var txSubscriber: TxSubscriber

    @Test
    fun `produced subscriber should filter and track txs wrapped by ExecutedContractTx`() {
        val txExistsCaptor = mutableListOf<Tx>()
        val txTrackCaptor = mutableListOf<Tx>()
        val sampleTxList: MutableList<Tx> = prepareSampleTxList()
        val idsToBeTracked = sampleTxList.map { it.id.asBase58String() }.toSet()
        val listToBeSentToSubscriber = sampleTxList.map {
            if (it is ExecutableTx) {
                TestDataFactory.executedContractTx(id = TxId.fromByteArray(Util.randomBytesFromUUID()), tx = it)
            } else it
        }.toList()
        every {
            txTracker.existsInTracker(any())
        } returns true

        listToBeSentToSubscriber.forEach {
            txSubscriber.subscribe(it)
        }

        verify(exactly = listToBeSentToSubscriber.size) {
            txTracker.existsInTracker(capture(txExistsCaptor))
        }

        verify(exactly = listToBeSentToSubscriber.size) {
            txTracker.setTrackStatus(capture(txTrackCaptor), eq(TxTrackStatus.SUCCESS))
        }

        assertEquals(idsToBeTracked.size, getIdsForTxList(txExistsCaptor).size)
        assertEquals(idsToBeTracked.size, getIdsForTxList(txTrackCaptor).size)
    }

    private fun getIdsForTxList(txList: List<Tx>): Set<String> = txList.map { it.id.asBase58String() }.toSet()

    private fun prepareSampleTxList(): MutableList<Tx> {
        val sampleTxList: MutableList<Tx> = (1..4).map {
            TestDataFactory.callContractTx(id = TxId.fromByteArray("id100$it".toByteArray()))
        }.toMutableList()
        sampleTxList += TestDataFactory.permitTx()
        return sampleTxList
    }
}
