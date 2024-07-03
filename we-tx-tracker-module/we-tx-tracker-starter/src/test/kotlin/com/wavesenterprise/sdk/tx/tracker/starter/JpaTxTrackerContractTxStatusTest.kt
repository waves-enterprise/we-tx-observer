package com.wavesenterprise.sdk.tx.tracker.starter

import com.ninjasquad.springmockk.MockkBean
import com.wavesenterprise.sdk.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.node.client.blocking.tx.TxService
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.contract.ContractTxStatus.Companion.FATAL_ERROR_CODE
import com.wavesenterprise.sdk.node.domain.contract.ContractTxStatus.Companion.RECOVERABLE_ERROR_CODE
import com.wavesenterprise.sdk.node.domain.contract.TxStatus
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.node.test.data.Util.Companion.randomBytesFromUUID
import com.wavesenterprise.sdk.tx.observer.common.jpa.util.flushAndClear
import com.wavesenterprise.sdk.tx.tracker.api.TxTracker
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackInfo
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackStatus
import com.wavesenterprise.sdk.tx.tracker.jpa.TxTrackerJpaAutoConfig
import com.wavesenterprise.sdk.tx.tracker.jpa.repository.BusinessObjectInfoJpaRepository
import com.wavesenterprise.sdk.tx.tracker.jpa.repository.SmartContractInfoJpaRepository
import com.wavesenterprise.sdk.tx.tracker.jpa.repository.TxTrackerJpaRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TestTransaction

@DataJpaTest(
    properties = [
        "tx-tracker.enabled = true",
        "tx-tracker.min-contract-tx-error-count = 2",
        "tx-tracker.min-contract-tx-failure-count = 2",
        "tx-tracker.fail-on-recoverable-contract-tx-error = false"
    ]
)
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        DataSourceAutoConfiguration::class,
        TxTrackerJpaAutoConfig::class,
        TxTrackerConfig::class,
        FlywaySchemaConfiguration::class,
    ]
)
// fixme should work without DataSourceAutoConfiguration (has in wired starters)
// but fails on dataSource bean condition (consider ordering)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
internal class JpaTxTrackerContractTxStatusTest {

    @PersistenceContext
    lateinit var em: EntityManager

    @Autowired
    lateinit var txTracker: TxTracker

    @Autowired
    lateinit var smartContractInfoJpaRepository: SmartContractInfoJpaRepository

    @Autowired
    lateinit var txTrackInfoRepository: TxTrackerJpaRepository

    @Autowired
    lateinit var businessObjectInfoJpaRepository: BusinessObjectInfoJpaRepository

    @MockkBean
    lateinit var txService: TxService

    @MockkBean(relaxed = true)
    lateinit var nodeBlockingServiceFactory: NodeBlockingServiceFactory

    private val createContractTx = TestDataFactory.createContractTx(id = TxId.fromByteArray(randomBytesFromUUID()))

    private val contractTxStatus = TestDataFactory.contractTxStatus(
        status = TxStatus.ERROR,
    )

    @AfterEach
    fun cleanData() {
        if (TestTransaction.isFlaggedForRollback()) {
            return
        }
        if (!TestTransaction.isActive()) {
            TestTransaction.start()
        }
        businessObjectInfoJpaRepository.deleteAll()
        txTrackInfoRepository.deleteAll()
        smartContractInfoJpaRepository.deleteAll()
        TestTransaction.flagForCommit()
        TestTransaction.end()
    }

    @Test
    fun `should do nothing with status when having only recoverable errors`() {
        val txId = TxId.fromByteArray(randomBytesFromUUID())
        val tx: Tx = createContractTx.copy(id = txId)
        val contractTxStatusList = (1..4).map { i ->
            contractTxStatus.copy(
                txId = txId,
                status = TxStatus.ERROR,
                code = if (i <= 2) null else RECOVERABLE_ERROR_CODE
            )
        }

        txTracker.trackTx(tx)
        txTracker.setContractTxError(tx.id, contractTxStatusList)

        em.flushAndClear()

        val txTrackInfoForTx = em.find(TxTrackInfo::class.java, tx.id.asBase58String())
        assertNotNull(txTrackInfoForTx.errors)
        assertEquals(contractTxStatusList.size, txTrackInfoForTx.errors?.size())
        assertEquals(TxTrackStatus.PENDING, txTrackInfoForTx.status)
    }

    @Test
    fun `should do nothing with status when error count lower than need to fail`() {
        val txId = TxId.fromByteArray(randomBytesFromUUID())
        val tx: Tx = createContractTx.copy(id = txId)
        val contractTxStatusList = listOf(
            contractTxStatus.copy(txId = txId, status = TxStatus.ERROR, code = FATAL_ERROR_CODE),
            contractTxStatus.copy(txId = txId, status = TxStatus.FAILURE)
        )

        txTracker.trackTx(tx)
        txTracker.setContractTxError(tx.id, contractTxStatusList)

        em.flushAndClear()

        val txTrackInfoForTx = em.find(TxTrackInfo::class.java, tx.id.asBase58String())
        assertNotNull(txTrackInfoForTx.errors)
        assertEquals(contractTxStatusList.size, txTrackInfoForTx.errors?.size())
        assertEquals(TxTrackStatus.PENDING, txTrackInfoForTx.status)
    }
}
