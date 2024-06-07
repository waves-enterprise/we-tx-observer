package com.wavesenterprise.sdk.tx.observer.starter.observer.web

import com.wavesenterprise.sdk.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.sdk.node.client.http.tx.CallContractTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.client.http.tx.CreateContractTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.tx.observer.core.spring.web.TxQueuePartitionController
import com.wavesenterprise.sdk.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.sdk.tx.observer.domain.TxQueuePartition
import com.wavesenterprise.sdk.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.sdk.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.sdk.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.sdk.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.sdk.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration
import com.wavesenterprise.sdk.tx.observer.starter.observer.util.ModelFactory.enqueuedTx
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.everyItem
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.lessThan
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

@WebMvcTest(controllers = [TxQueuePartitionController::class])
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        DataSourceAutoConfiguration::class,
        TxObserverJpaAutoConfig::class,
        NodeBlockingServiceFactoryMockConfiguration::class,
        TxObserverStarterConfig::class,
        FlywaySchemaConfiguration::class,
        TxObserverJpaConfig::class,
    ]
)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureDataJpa
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
internal class TxQueuePartitionControllerTest {

    @Autowired
    lateinit var txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository

    @Autowired
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    @Autowired
    lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        enqueuedTxJpaRepository.deleteAll()
        txQueuePartitionJpaRepository.deleteAll()
    }

    @AfterEach
    fun tearDown() {
        enqueuedTxJpaRepository.deleteAll()
        txQueuePartitionJpaRepository.deleteAll()
    }

    @Test
    fun `get partitions should return filtered by priority equals`() {
        val filterPriority = 10
        val filterPartitionCount = 1
        (0..9).map {
            txQueuePartitionJpaRepository.save(
                TxQueuePartition(
                    id = "id_$it",
                    priority = if (it < filterPartitionCount) filterPriority else 0
                )
            )
        }

        mockMvc.get("/observer/partitions") {
            param("priority", filterPriority.toString())
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.content", hasSize<Any>(filterPartitionCount))
            jsonPath("$.content[*].priority", everyItem(equalTo(filterPriority)))
        }
    }

    @Test
    fun `get partitions should return filtered by priority lower than`() {
        val filterPriority = 10
        val filterPartitionCount = 2
        (0..9).map {
            txQueuePartitionJpaRepository.save(
                TxQueuePartition(
                    id = "id_$it",
                    priority = if (it < filterPartitionCount) filterPriority - 1 else filterPriority + 1
                )
            )
        }

        mockMvc.get("/observer/partitions") {
            param("priority", "$filterPriority")
            param("priorityOp", "LT")
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.content", hasSize<Any>(filterPartitionCount))
            jsonPath("$.content[*].priority", everyItem(lessThan(filterPriority)))
        }
    }

    @Test
    fun `get partitions should return filtered by priority grater than`() {
        val filterPriority = 10
        val filterPartitionCount = 3
        (0..9).map {
            txQueuePartitionJpaRepository.save(
                TxQueuePartition(
                    id = "id_$it",
                    priority = if (it < filterPartitionCount) filterPriority + 1 else filterPriority - 1
                )
            )
        }

        mockMvc.get("/observer/partitions") {
            param("priority", "$filterPriority")
            param("priorityOp", "GT")
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.content", hasSize<Any>(filterPartitionCount))
            jsonPath("$.content[*].priority", everyItem(greaterThan(filterPriority)))
        }
    }

    @Test
    fun `get partitions should return filtered by status`() {
        val filterByStatus = EnqueuedTxStatus.READ
        val filterPartitionCount = 4
        (0..9).map {
            val partition = txQueuePartitionJpaRepository.save(
                TxQueuePartition(
                    id = "id_part_$it",
                    priority = 0
                )
            )
            enqueuedTxJpaRepository.save(
                enqueuedTx(
                    tx = TestDataFactory.createContractTx(id = TxId.fromByteArray("id_tx_$it".toByteArray())).toDto(),
                    status = if (it < filterPartitionCount) filterByStatus else EnqueuedTxStatus.NEW,
                    partition = partition,
                    positionInBlock = 0
                )
            )
        }

        mockMvc.get("/observer/partitions") {
            param("status", filterByStatus.name)
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.content", hasSize<Any>(filterPartitionCount))
        }
    }

    @Test
    fun `get partitions should return only active`() {
        val activePartitionCount = 6
        (0..9).map { num ->
            txQueuePartitionJpaRepository.save(
                TxQueuePartition(
                    id = "id_$num",
                    priority = 0
                )
            ).takeIf { num < activePartitionCount }?.apply {
                enqueuedTxJpaRepository.save(
                    enqueuedTx(
                        tx = TestDataFactory.callContractTx(id = TxId.fromByteArray("${UUID.randomUUID()}".toByteArray())).toDto(),
                        partition = this
                    )
                )
            }
        }

        mockMvc.get("/observer/partitions") {
            param("active", "true")
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.content", hasSize<Any>(activePartitionCount))
            jsonPath("$.content[*].lastEnqueuedTxId", everyItem(equalTo("txId")))
            jsonPath("$.content[*].lastReadTxId", everyItem(nullValue()))
        }
    }

    @Test
    fun `get partitions should return only inactive`() {
        val inactivePartitionCount = 7
        (0..9).map { num ->
            txQueuePartitionJpaRepository.save(
                TxQueuePartition(
                    id = "id_$num",
                    priority = 0
                )
            ).takeIf { num >= inactivePartitionCount }?.apply {
                enqueuedTxJpaRepository.save(
                    enqueuedTx(
                        tx = TestDataFactory.callContractTx(id = TxId.fromByteArray("${UUID.randomUUID()}".toByteArray())).toDto(),
                        partition = this
                    )
                )
            }
        }

        mockMvc.get("/observer/partitions") {
            param("active", "false")
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.content", hasSize<Any>(inactivePartitionCount))
            jsonPath("$.content[*].lastEnqueuedTxId", everyItem(equalTo("txId")))
            jsonPath("$.content[*].lastReadTxId", everyItem(equalTo("txId")))
        }
    }

    @Test
    fun `get partition by id should return existing partition`() {
        val partitionId = "partition_id"
        txQueuePartitionJpaRepository.save(
            TxQueuePartition(
                id = partitionId,
                priority = 0
            )
        )

        mockMvc.get("/observer/partitions/{partitionId}", partitionId).andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.id", equalTo(partitionId))
        }
    }

    @Test
    fun `get partition by id should return error 404 if partition doesnt exists`() {
        mockMvc.get("/observer/partitions/{partitionId}", "unavailable").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `get status endpoint should return error partitions count and total partitions count`() {
        val errorPriority = -1
        val errorPartitionCount = 8
        val totalPartitionCount = 10

        (0 until totalPartitionCount).map {
            txQueuePartitionJpaRepository.save(
                TxQueuePartition(
                    id = "id_$it",
                    priority = if (it < errorPartitionCount) errorPriority else 0
                )
            ).apply {
                enqueuedTxJpaRepository.save(
                    enqueuedTx(
                        tx = TestDataFactory.callContractTx(id = TxId.fromByteArray("${UUID.randomUUID()}".toByteArray())).toDto(),
                        partition = this
                    )
                )
            }
        }

        mockMvc.get("/observer/partitions/status").andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json("{\"errorPartitionCount\":$errorPartitionCount,\"totalPartitionCount\":$totalPartitionCount}") }
        }
    }
}
