package com.wavesenterprise.we.tx.observer.starter.observer.executor.poller

import com.ninjasquad.springmockk.MockkBean
import com.wavesenterprise.we.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.we.tx.observer.common.tx.executor.TxExecutor
import com.wavesenterprise.we.tx.observer.common.tx.executor.TxExecutorImpl
import com.wavesenterprise.we.tx.observer.core.spring.executor.poller.ScheduledBlockInfoSynchronizer
import com.wavesenterprise.we.tx.observer.core.spring.executor.poller.SourceExecutor
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.we.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.we.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CyclicBarrier
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.concurrent.thread

@ActiveProfiles("test")
@EnableAutoConfiguration
@SpringBootTest(
    classes = [
        DataSourceAutoConfiguration::class,
        TxObserverJpaAutoConfig::class,
        TxObserverJpaConfig::class,
        TxExecutorImpl::class,
        FlywaySchemaConfiguration::class,
        NodeBlockingServiceFactoryMockConfiguration::class
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MockkBean(
    TransactionTemplate::class,
)
class ScheduledWeBlockInfoSynchronizerOptimisticLockTest {

    @MockkBean
    lateinit var sourceExecutor: SourceExecutor

    @Autowired
    lateinit var syncInfoService: SyncInfoService

    @Autowired
    lateinit var txExecutor: TxExecutor

    @PersistenceContext
    lateinit var em: EntityManager

    @Volatile
    var exception: Exception? = null

    lateinit var scheduledBlockInfoSynchronizer: ScheduledBlockInfoSynchronizer

    @BeforeEach
    fun setUp() {
        scheduledBlockInfoSynchronizer = ScheduledBlockInfoSynchronizer(
            sourceExecutor = sourceExecutor,
            txExecutor = txExecutor,
            syncInfoService = syncInfoService,
            liquidBlockPollingDelay = 1,
            blockHeightWindow = 1
        )
        every { sourceExecutor.execute(any(), any()) } returns 3L
    }

    @Test
    fun `should do nothing when 2 threads is crossing`() {
        val cyclicBarrier = CyclicBarrier(2)

        val firstThread = thread {
            try {
                cyclicBarrier.await()
                scheduledBlockInfoSynchronizer.syncNodeBlockInfo()
            } catch (ex: Exception) {
                exception = ex
            }
        }
        val secondThread = thread {
            try {
                cyclicBarrier.await()
                scheduledBlockInfoSynchronizer.syncNodeBlockInfo()
            } catch (ex: Exception) {
                exception = ex
            }
        }

        firstThread.join()
        secondThread.join()
        assertNull(exception)
    }
}
