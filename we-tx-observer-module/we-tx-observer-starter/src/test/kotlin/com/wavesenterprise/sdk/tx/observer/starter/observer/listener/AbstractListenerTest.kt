package com.wavesenterprise.sdk.tx.observer.starter.observer.listener

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import com.wavesenterprise.sdk.node.client.blocking.address.AddressService
import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.sdk.node.client.blocking.contract.ContractService
import com.wavesenterprise.sdk.node.client.blocking.event.BlockchainEventsService
import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.node.client.blocking.node.NodeInfoService
import com.wavesenterprise.sdk.node.client.blocking.privacy.PrivacyService
import com.wavesenterprise.sdk.node.client.blocking.tx.TxService
import com.wavesenterprise.sdk.node.client.feign.tx.mapDto
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.tx.observer.core.spring.partition.PartitionHandler
import com.wavesenterprise.sdk.tx.observer.core.spring.partition.PollingTxSubscriber
import com.wavesenterprise.sdk.tx.observer.domain.EnqueuedTx
import com.wavesenterprise.sdk.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.sdk.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.sdk.tx.observer.jpa.repository.BlockHeightResetRepository
import com.wavesenterprise.sdk.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.sdk.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.sdk.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.BlockListenerTestContextConfiguration
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.ObjectMapperConfig
import com.wavesenterprise.sdk.tx.observer.starter.observer.util.ModelFactory.enqueuedTx
import com.wavesenterprise.sdk.tx.observer.starter.observer.web.service.TransactionalRunner
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    classes = [
        ObjectMapperConfig::class,
        BlockListenerTestContextConfiguration::class,
        TxObserverJpaAutoConfig::class,
        TxObserverStarterConfig::class,
    ],
    properties = ["tx-observer.tx-poller.size = 100"],
)
@ActiveProfiles("test")
@MockBean(TransactionalRunner::class, PartitionHandler::class)
abstract class AbstractListenerTest {

    @Autowired
    lateinit var nodeBlockingServiceFactory: NodeBlockingServiceFactory

    @Autowired
    lateinit var addressService: AddressService

    @Autowired
    lateinit var blockchainEventsService: BlockchainEventsService

    @Autowired
    lateinit var blocksService: BlocksService

    @Autowired
    lateinit var contractService: ContractService

    @Autowired
    lateinit var nodeInfoService: NodeInfoService

    @Autowired
    lateinit var privacyService: PrivacyService

    @Autowired
    lateinit var txService: TxService

    @Autowired
    lateinit var pollingTxSubscriber: PollingTxSubscriber

    @MockkBean(relaxed = true)
    lateinit var txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository

    @MockkBean(relaxed = true)
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    @MockkBean(relaxed = true)
    lateinit var blockHeightResetRepository: BlockHeightResetRepository

    lateinit var enqueuedTxList: MutableList<EnqueuedTx>
    val mockPartitionId = "partId"
    val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setUp() {
        enqueuedTxList = mutableListOf()
        every {
            enqueuedTxJpaRepository.findActualEnqueuedTxForPartition(
                enqueuedTxStatus = EnqueuedTxStatus.NEW,
                partitionId = mockPartitionId,
                pageable = PageRequest.of(0, 100),
            )
        } answers { PageImpl(enqueuedTxList) }
    }

    fun enqueue(txs: List<Tx>) =
        enqueue(*txs.toTypedArray())

    fun enqueue(vararg txs: Tx) =
        txs.map {
            enqueuedTx(
                tx = mapDto(it),
                partition = NodeBlockingServiceFactoryMockConfiguration.mockPartition,
            )
        }.also {
            enqueuedTxList.addAll(it)
        }
}
