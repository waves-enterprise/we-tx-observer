package com.wavesenterprise.we.tx.observer.starter.observer.web

import com.wavesenterprise.sdk.node.client.http.tx.CreateContractTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.client.http.tx.PolicyDataHashTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.TxType
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.we.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.we.tx.observer.core.spring.web.EnqueuedTxController
import com.wavesenterprise.we.tx.observer.domain.TxQueuePartition
import com.wavesenterprise.we.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.we.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.we.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.we.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration
import com.wavesenterprise.we.tx.observer.starter.observer.util.ModelFactory.enqueuedTx
import org.hamcrest.Matchers.hasSize
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
import javax.persistence.EntityManager

@WebMvcTest(controllers = [EnqueuedTxController::class])
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
internal class EnqueuedTxControllerTest {

    @Autowired
    lateinit var txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository

    @Autowired
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var em: EntityManager

    @BeforeEach
    fun setUp() {
        enqueuedTxJpaRepository.deleteAll()
        txQueuePartitionJpaRepository.deleteAll()
    }

    @Test
    fun `should filter by available and txType`() {
        val partitionId = "some_partition_id"
        val partition = txQueuePartitionJpaRepository.save(
            TxQueuePartition(
                id = partitionId,
                priority = 0
            )
        )
        var i = 0
        listOf(
            enqueuedTx(
                tx = TestDataFactory.policyDataHashTx(id = TxId.fromByteArray("${i++}".toByteArray())).toDto(),
                partition = partition
            ),
            enqueuedTx(
                tx = TestDataFactory.policyDataHashTx(id = TxId.fromByteArray("${i++}".toByteArray())).toDto(),
                partition = partition
            ),
            enqueuedTx(
                tx = TestDataFactory.policyDataHashTx(id = TxId.fromByteArray("${i++}".toByteArray())).toDto(),
                partition = partition,
                available = false
            ),
            enqueuedTx(
                tx = TestDataFactory.createContractTx(id = TxId.fromByteArray("${i++}".toByteArray())).toDto(),
                partition = partition
            ),
            enqueuedTx(
                tx = TestDataFactory.createContractTx(id = TxId.fromByteArray("$i".toByteArray())).toDto(),
                partition = partition
            )
        ).onEach {
            enqueuedTxJpaRepository.save(it)
        }

        mockMvc.get("/observer/queue") {
            param("txType", TxType.POLICY_DATA_HASH.code.toString())
            param("available", "true")
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.content", hasSize<Any>(2))
        }
    }

    @Test
    fun `should return tx filtered by partition id`() {
        val partitionId = "some_partition_id"
        val partition = txQueuePartitionJpaRepository.save(
            TxQueuePartition(
                id = partitionId,
                priority = 0
            )
        )
        val anotherPartition = txQueuePartitionJpaRepository.save(
            TxQueuePartition(
                id = "another_partition_id",
                priority = 0
            )
        )

        val enqueuedTxInPartitionCount = 4
        (0..9).map {
            enqueuedTxJpaRepository.save(
                enqueuedTx(
                    tx = TestDataFactory.createContractTx(id = TxId.fromByteArray("id_$it".toByteArray())).toDto(),
                    positionInBlock = it,
                    blockHeight = 100,
                    partition = if (it < enqueuedTxInPartitionCount) partition else anotherPartition
                )
            )
        }

        mockMvc.get("/observer/queue") {
            param("partitionId", partitionId)
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.content", hasSize<Any>(enqueuedTxInPartitionCount))
        }
    }
}
