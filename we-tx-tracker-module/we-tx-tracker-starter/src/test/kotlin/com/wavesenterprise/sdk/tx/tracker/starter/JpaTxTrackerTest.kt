package com.wavesenterprise.sdk.tx.tracker.starter

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import com.wavesenterprise.sdk.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.node.client.blocking.tx.TxService
import com.wavesenterprise.sdk.node.client.http.tx.AtomicInnerTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.domain.Address
import com.wavesenterprise.sdk.node.domain.PublicKey
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.Timestamp
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.TxType
import com.wavesenterprise.sdk.node.domain.contract.ContractId
import com.wavesenterprise.sdk.node.domain.contract.ContractId.Companion.contractId
import com.wavesenterprise.sdk.node.domain.contract.ContractTxStatus
import com.wavesenterprise.sdk.node.domain.contract.TxStatus
import com.wavesenterprise.sdk.node.domain.tx.ContractTx
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.domain.tx.TxInfo
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.node.test.data.Util.Companion.randomBytesFromUUID
import com.wavesenterprise.sdk.node.test.data.Util.Companion.randomStringBase58
import com.wavesenterprise.sdk.tx.observer.common.jpa.util.flushAndClear
import com.wavesenterprise.sdk.tx.tracker.api.TxTracker
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackBusinessObjectInfo
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackInfo
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackStatus
import com.wavesenterprise.sdk.tx.tracker.jpa.TxTrackerJpaAutoConfig
import com.wavesenterprise.sdk.tx.tracker.jpa.config.TxTrackerJpaConfig
import com.wavesenterprise.sdk.tx.tracker.jpa.repository.BusinessObjectInfoJpaRepository
import com.wavesenterprise.sdk.tx.tracker.jpa.repository.SmartContractInfoJpaRepository
import com.wavesenterprise.sdk.tx.tracker.jpa.repository.TxTrackerJpaRepository
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.util.Optional
import java.util.concurrent.CountDownLatch

@DataJpaTest(properties = ["tx-tracker.enabled = true"])
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        DataSourceAutoConfiguration::class,
        TxTrackerJpaAutoConfig::class,
        TxTrackerConfig::class,
        FlywaySchemaConfiguration::class,
        TxTrackerJpaConfig::class,
    ],
)
// fixme should work without DataSourceAutoConfiguration (has in wired starters)
// but fails on dataSource bean condition (consider ordering)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
internal class JpaTxTrackerTest {

    @PersistenceContext
    lateinit var em: EntityManager

    @Autowired
    lateinit var txTracker: TxTracker

    @MockkBean
    lateinit var txService: TxService

    @MockkBean(relaxed = true)
    lateinit var nodeBlockingServiceFactory: NodeBlockingServiceFactory

    @Autowired
    lateinit var smartContractInfoJpaRepository: SmartContractInfoJpaRepository

    @Autowired
    lateinit var txTrackInfoRepository: TxTrackerJpaRepository

    @Autowired
    lateinit var businessObjectInfoJpaRepository: BusinessObjectInfoJpaRepository

    @Autowired
    lateinit var transactionManager: PlatformTransactionManager

    val objectMapper = jacksonObjectMapper()

    val callContractTx = TestDataFactory.callContractTx(id = TxId.fromByteArray("id1001".toByteArray()))
    val createContractTx = TestDataFactory.createContractTx(id = TxId.fromByteArray("id1002".toByteArray()))
    val disableContractTx = TestDataFactory.disableContractTx(id = TxId.fromByteArray("id1003".toByteArray()))
    val updateContractTx = TestDataFactory.updateContractTx(id = TxId.fromByteArray("id1004".toByteArray()))
    val createPolicyTx = TestDataFactory.createPolicyTx(id = TxId.fromByteArray("id1005".toByteArray()))

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
    fun `should track createPolicyTx tx`() {
        txTracker.trackTx(createPolicyTx)

        em.flushAndClear()

        em.find(TxTrackInfo::class.java, createPolicyTx.id.asBase58String()).apply {
            assertEquals(112, type)
            assertEquals(TxTrackStatus.PENDING, status)
            assertThatJson(createPolicyTx.toDto()).isEqualTo(body)
            assertNull(errors)
            assertNull(smartContractInfo)
        }
    }

    @Test
    fun `should track Tx by saving it and the corresponding smart contract`() {
        txTracker.trackTx(createContractTx)

        em.flushAndClear()

        val txTrackInfoForTx = em.find(TxTrackInfo::class.java, createContractTx.id.asBase58String())
        assertEquals(TxTrackStatus.PENDING, txTrackInfoForTx.status)
        assertThatJson(createContractTx.toDto()).isEqualTo(txTrackInfoForTx.body)
        assertNull(txTrackInfoForTx.errors)
        assertNotNull(txTrackInfoForTx.smartContractInfo)
        txTrackInfoForTx.smartContractInfo!!.apply {
            assertEquals(createContractTx.id.asBase58String(), id)
            assertEquals(createContractTx.image.value, image)
            assertEquals(createContractTx.imageHash.value, imageHash)
            assertEquals(createContractTx.contractName.value, contractName)
            assertEquals(createContractTx.senderAddress.asBase58String(), sender)
            assertEquals(createContractTx.version.value, version)
        }
    }

    @Test
    fun `should track CallContractTx when having tracked corresponding CreateContractTx`() {
        txTracker.trackTx(createContractTx)
        txTracker.trackTx(callContractTx.copy(contractId = ContractId(createContractTx.id)))

        em.flushAndClear()

        val txTrackInfoForCreate = em.find(TxTrackInfo::class.java, createContractTx.id.asBase58String())
        val txTrackInfoForCall = em.find(TxTrackInfo::class.java, callContractTx.id.asBase58String())
        assertNotNull(txTrackInfoForCreate)
        assertNotNull(txTrackInfoForCall)
        assertEquals(txTrackInfoForCreate.smartContractInfo, txTrackInfoForCall.smartContractInfo)
    }

    @Test
    fun `should track DisableContractTx when having tracked corresponding CreateContractTx`() {
        txTracker.trackTx(createContractTx)
        val disableContractTx = disableContractTx.copy(contractId = createContractTx.id.contractId)
        txTracker.trackTx(disableContractTx)

        em.flushAndClear()

        val txTrackInfoForCreate = em.find(TxTrackInfo::class.java, createContractTx.id.asBase58String())
        val txTrackInfoForDisable = em.find(TxTrackInfo::class.java, disableContractTx.id.asBase58String())
        assertNotNull(txTrackInfoForCreate)
        assertNotNull(txTrackInfoForDisable)
        assertEquals(106, txTrackInfoForDisable.type)
        assertThatJson(disableContractTx.toDto()).isEqualTo(txTrackInfoForDisable.body)
        assertEquals(txTrackInfoForCreate.smartContractInfo, txTrackInfoForDisable.smartContractInfo)
    }

    @Test
    fun `should track UpdateContractTx when having tracked corresponding CreateContractTx`() {
        txTracker.trackTx(createContractTx)
        val updateContractTx = updateContractTx.copy(contractId = createContractTx.id.contractId)
        txTracker.trackTx(updateContractTx)

        em.flushAndClear()

        val txTrackInfoForCreate = em.find(TxTrackInfo::class.java, createContractTx.id.asBase58String())
        val txTrackInfoForUpdate = em.find(TxTrackInfo::class.java, updateContractTx.id.asBase58String())
        assertNotNull(txTrackInfoForCreate)
        assertNotNull(txTrackInfoForUpdate)
        assertEquals(107, txTrackInfoForUpdate.type)
        assertThatJson(updateContractTx.toDto()).isEqualTo(txTrackInfoForUpdate.body)
        assertEquals(txTrackInfoForCreate.smartContractInfo, txTrackInfoForUpdate.smartContractInfo)
    }

    @Test
    fun `should support repeatable tracking of the same tx`() {
        txTracker.trackTx(createContractTx)
        val txToBeTracked = callContractTx.copy(contractId = createContractTx.id.contractId)
        val businessObjectInfos = listOf(TxTrackBusinessObjectInfo("id", "OBJECT"))
        val txTrackInfoFirstInvocation = txTracker.trackTx(
            tx = txToBeTracked,
            businessObjectInfos = businessObjectInfos,
        )

        em.flushAndClear()

        val trackTxInfoSecondInvocation = txTracker.trackTx(
            tx = txToBeTracked,
            businessObjectInfos = businessObjectInfos,
        )
        em.flush()

        assertEquals(txTrackInfoFirstInvocation.id, trackTxInfoSecondInvocation.id)
    }

    @Test
    fun `should support repeatable parallel tracking of the same tx`() {
        txTracker.trackTx(createContractTx)
        TestTransaction.flagForCommit()
        TestTransaction.end()
        val txToBeTracked = callContractTx.copy(contractId = createContractTx.id.contractId)
        val businessObjectInfos = listOf(TxTrackBusinessObjectInfo("id", "OBJECT"))

        val countDownLatch = CountDownLatch(2)
        val transactionTemplate = TransactionTemplate(transactionManager)
        transactionTemplate.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        val trackInfos = mutableListOf<TxTrackInfo>()
        (1..2).map {
            Thread {
                val txTrackInfo = transactionTemplate.execute {
                    txTracker.trackTx(
                        tx = txToBeTracked,
                        businessObjectInfos = businessObjectInfos,
                    ).also {
                        em.flushAndClear()

                        countDownLatch.countDown()
                    }
                }
                txTrackInfo?.also {
                    trackInfos.add(it)
                }
            }.apply {
                start()
            }
        }.forEach {
            it.join()
        }

        assertEquals(2, trackInfos.size)
        assertEquals(trackInfos[0].id, trackInfos[1].id)
        val foundTxTrackInfo = txTrackInfoRepository.findByIdOrNull(trackInfos[0].id)
        assertNotNull(foundTxTrackInfo)
    }

    @Test
    fun `should throw exception when corresponding smartContractInfo doesn't exist`() {
        ReflectionTestUtils.setField(txTracker, "findContractInNode", false)
        val contractId = "some_not_existent_id"
        assertThrows<IllegalArgumentException>(
            "No smartContractInfo for tracked CallContractTx " +
                "(contractId = $contractId)",
        ) {
            txTracker.trackTx(callContractTx.copy(contractId = ContractId.fromByteArray(contractId.toByteArray())))
        }
    }

    @Test
    fun `should try to find contract info in node state`() {
        ReflectionTestUtils.setField(txTracker, "findContractInNode", true)
        val contractId = ContractId(TxId(randomBytesFromUUID()))
        val tx = callContractTx.copy(
            id = TxId(randomBytesFromUUID()),
            contractId = contractId,
        )
        every {
            txService.txInfo(contractId.txId)
        } returns Optional.of(
            TxInfo(
                height = mockk(),
                tx = createContractTx.copy(id = contractId.txId),
            ),
        )

        txTracker.trackTx(tx)

        em.flushAndClear()

        assertTrue(smartContractInfoJpaRepository.existsById(contractId.asBase58String()))
        assertTrue(txTracker.existsInTracker(tx))
    }

    @Test
    fun `should check for existence of Tx in tracker`() {
        val tx = createContractTx.copy(id = TxId.fromByteArray("newId".toByteArray()))
        val notTrackedTx: Tx = callContractTx.copy(id = TxId.fromByteArray("newNotTrackedTxId".toByteArray()))
        txTracker.trackTx(tx)

        em.flushAndClear()

        assertTrue(txTracker.existsInTracker(tx))
        assertFalse(txTracker.existsInTracker(notTrackedTx))
    }

    @Test
    fun `should check for existence of Tx in tracker by status`() {
        val txNotToBeFound: Tx = createContractTx.copy(id = TxId.fromByteArray("newId".toByteArray()))
        val tx: Tx = createContractTx.copy(id = TxId.fromByteArray("newIdWithPending".toByteArray()))
        val notTrackedTx: Tx = createContractTx.copy(id = TxId.fromByteArray("newNotTrackedTxId".toByteArray()))
        txTracker.trackTx(tx)
        txTracker.trackTx(txNotToBeFound)
        txTracker.setTrackStatus(tx, TxTrackStatus.SUCCESS)

        em.flushAndClear()

        assertTrue(txTracker.existsInTrackerWithStatus(tx, TxTrackStatus.SUCCESS))
        assertFalse(txTracker.existsInTrackerWithStatus(txNotToBeFound, TxTrackStatus.SUCCESS))
        assertFalse(txTracker.existsInTrackerWithStatus(notTrackedTx, TxTrackStatus.SUCCESS))
    }

    @Test
    fun `should set track status`() {
        val tx: Tx = createContractTx.copy(id = TxId.fromByteArray("newId".toByteArray()))
        txTracker.trackTx(tx)

        txTracker.setTrackStatus(tx, TxTrackStatus.SUCCESS)

        em.flushAndClear()

        val txTrackInfoForTx = em.find(TxTrackInfo::class.java, tx.id.asBase58String())
        assertEquals(TxTrackStatus.SUCCESS, txTrackInfoForTx.status)
    }

    @Test
    fun `should get tracked tx for certain status`() {
        val pendingTxMap = preparePendingTrackedTxs()

        em.flushAndClear()

        val trackedWithPending = txTracker.getTrackedTxWithStatus(TxTrackStatus.PENDING)
        assertEquals(pendingTxMap.size, trackedWithPending.size)
        trackedWithPending.forEach { actualTx ->
            val trackedTx = pendingTxMap[actualTx.id]
            assertNotNull(trackedTx)
            assertThatJson(actualTx).isEqualTo(trackedTx)
        }
    }

    @Test
    fun `should get tracked tx ids for certain status`() {
        val pendingTxMap = preparePendingTrackedTxs()

        em.flushAndClear()

        val trackedWithPending = txTracker.getTrackedTxIdsWithStatus(TxTrackStatus.PENDING)
        trackedWithPending.forEach { actualTxId ->
            val trackedTx = pendingTxMap[actualTxId]
            assertNotNull(trackedTx)
        }
    }

    @Test
    fun `should update error info`() {
        val txId = TxId.fromByteArray("id1".toByteArray())
        val tx: Tx = createContractTx.copy(id = txId)
        val contractTxStatusDto = ContractTxStatus(
            senderPublicKey = PublicKey("dd".toByteArray()),
            status = TxStatus.FAILURE,
            senderAddress = Address.fromByteArray("ess".toByteArray()),
            txId = txId,
            message = "ddd",
            timestamp = Timestamp(1),
            signature = Signature.fromByteArray("sign".toByteArray()),
        )
        val contractTxStatusList = (1..3)
            .map { contractTxStatusDto.copy(txId = TxId.fromByteArray("id$it".toByteArray())) }
            .toList()
        val expectedTxStatusDtoMap = contractTxStatusList
            .map { it.txId to it }.toMap()
        txTracker.trackTx(tx)
        txTracker.setContractTxError(tx.id, contractTxStatusList)

        em.flushAndClear()

        val txTrackInfoForTx = em.find(TxTrackInfo::class.java, tx.id.asBase58String())
        assertEquals(TxTrackStatus.FAILURE, txTrackInfoForTx.status)
        assertNotNull(txTrackInfoForTx.errors)
        val jacksonJavaType = objectMapper.typeFactory.constructType(
            object : TypeReference<List<ContractTxStatus>>() {},
        )

        val actualSavedTxStatusDtoList = objectMapper.readValue<List<ContractTxStatus>>(
            objectMapper.treeAsTokens(txTrackInfoForTx.errors!!),
            jacksonJavaType,
        )
        assertEquals(expectedTxStatusDtoMap.size, actualSavedTxStatusDtoList.size)
        actualSavedTxStatusDtoList.forEach {
            val txStatusDto = expectedTxStatusDtoMap[it.txId]
            assertNotNull(txStatusDto)
            assertThatJson(it).isEqualTo(txStatusDto)
        }
    }

    @Test
    fun `should do nothing with status when having at least one success`() {
        val txId = TxId.fromByteArray("id1".toByteArray())
        val tx: Tx = createContractTx.copy(id = txId)
        val contractTxStatusDto = ContractTxStatus(
            senderPublicKey = PublicKey("dd".toByteArray()),
            status = TxStatus.FAILURE,
            senderAddress = Address.fromByteArray("ess".toByteArray()),
            txId = txId,
            message = "ddd",
            timestamp = Timestamp(1),
            signature = Signature.fromByteArray("sign".toByteArray()),
        )
        val contractTxStatusDtoList = (1..4)
            .map {
                contractTxStatusDto.copy(
                    txId = TxId.fromByteArray("id$it".toByteArray()),
                    status = if (it == 4) TxStatus.SUCCESS else TxStatus.ERROR,
                )
            }
            .toList()
        txTracker.trackTx(tx)
        txTracker.setContractTxError(tx.id, contractTxStatusDtoList)

        em.flushAndClear()

        val txTrackInfoForTx = em.find(TxTrackInfo::class.java, tx.id.asBase58String())
        assertNotNull(txTrackInfoForTx.errors)
        assertEquals(contractTxStatusDtoList.size, txTrackInfoForTx.errors?.size())
        assertEquals(TxTrackStatus.PENDING, txTrackInfoForTx.status)
    }

    @Test
    fun `should set to TxTrackStatus-Error when having Error and Failure`() {
        val txId = TxId.fromByteArray("id1".toByteArray())
        val tx: Tx = createContractTx.copy(id = txId)
        val contractTxStatusDto = ContractTxStatus(
            senderPublicKey = PublicKey("dd".toByteArray()),
            status = TxStatus.FAILURE,
            senderAddress = Address.fromByteArray("dd".toByteArray()),
            txId = txId,
            message = "ddd",
            timestamp = Timestamp(1),
            signature = Signature.fromByteArray("sign".toByteArray()),
        )
        val contractTxStatusDtoList = (1..2)
            .map {
                contractTxStatusDto.copy(
                    txId = TxId.fromByteArray("id$it".toByteArray()),
                    status = if (it == 1) TxStatus.ERROR else TxStatus.FAILURE,
                )
            }
            .toList()
        txTracker.trackTx(tx)
        txTracker.setContractTxError(tx.id, contractTxStatusDtoList)

        em.flushAndClear()

        val txTrackInfoForTx = em.find(TxTrackInfo::class.java, tx.id.asBase58String())
        assertNotNull(txTrackInfoForTx.errors)
        assertEquals(contractTxStatusDtoList.size, txTrackInfoForTx.errors?.size())
        assertEquals(TxTrackStatus.ERROR, txTrackInfoForTx.status)
    }

    @Test
    fun `getTrackedTxIdsWithStatusAndTypes should contains ids after track CallContractTx`() {
        txTracker.trackTx(createContractTx)
        txTracker.trackTx(callContractTx.copy(contractId = ContractId(createContractTx.id)))

        em.flushAndClear()

        val result = txTracker.getTrackedTxIdsWithStatusAndTypes(
            txTrackerStatus = TxTrackStatus.PENDING,
            types = listOf(TxType.CALL_CONTRACT.code, TxType.CREATE_CONTRACT.code),
            pageRequest = PageRequest.of(0, 1000),
        )

        val txTrackInfoForCreate = em.find(TxTrackInfo::class.java, createContractTx.id.asBase58String())
        val txTrackInfoForCall = em.find(TxTrackInfo::class.java, callContractTx.id.asBase58String())
        assertNotNull(txTrackInfoForCreate)
        assertNotNull(txTrackInfoForCall)
        assertEquals(txTrackInfoForCreate.smartContractInfo, txTrackInfoForCall.smartContractInfo)
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertTrue(result.size == 2)
        assertTrue(result[0] == createContractTx.id)
        assertTrue(result[1] == callContractTx.id)
    }

    @Test
    fun `should return last tracked tx for business object with status`() {
        val businessObjectId = "testId"
        val businessObjectInfo = TxTrackBusinessObjectInfo(businessObjectId, "TEST_TYPE")
        val expectedTxId = TxId.fromBase58(randomStringBase58())

        txTracker.trackTx(
            tx = createContractTx.copy(id = TxId.fromBase58(randomStringBase58())),
            businessObjectInfos = listOf(businessObjectInfo),
        )
        em.flushAndClear()

        txTracker.trackTx(
            tx = createContractTx.copy(id = expectedTxId),
            businessObjectInfos = listOf(businessObjectInfo),
        )
        em.flushAndClear()

        createContractTx.copy(id = TxId.fromBase58(randomStringBase58())).apply {
            txTracker.trackTx(this, businessObjectInfos = listOf(businessObjectInfo))
            txTracker.setTrackStatus(this, TxTrackStatus.SUCCESS)
        }
        em.flushAndClear()

        val result = txTracker.getLastTrackedTxForBusinessObjectWithStatus(businessObjectId, TxTrackStatus.PENDING)

        assertNotNull(result.orElse(null))
        result.get().apply {
            assertEquals(expectedTxId.asBase58String(), id)
            assertEquals(TxTrackStatus.PENDING, status)
        }
    }

    private fun preparePendingTrackedTxs(): Map<TxId, ContractTx> {
        val pendingTxMap = (1..3).map { i ->
            createContractTx.copy(id = TxId.fromByteArray("newId_$i".toByteArray()))
                .also { txTracker.trackTx(it) }
        }.associateBy { it.id }
        val txWithSuccess = createContractTx.copy(id = TxId.fromByteArray("newId_4".toByteArray()))
        txTracker.trackTx(txWithSuccess)
        txTracker.setTrackStatus(txWithSuccess, TxTrackStatus.SUCCESS)
        return pendingTxMap
    }
}
