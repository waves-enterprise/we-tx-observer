package com.wavesenterprise.we.tx.observer.starter.observer.enabler

import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.we.tx.observer.api.partition.TxQueuePartitionResolver
import com.wavesenterprise.we.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.we.tx.observer.api.tx.TxEnqueuePredicate
import com.wavesenterprise.we.tx.observer.core.spring.method.callback.PrivateContentResolverImpl
import com.wavesenterprise.we.tx.observer.core.spring.partition.DefaultPartitionResolver
import com.wavesenterprise.we.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.we.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.we.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.we.tx.observer.starter.annotation.EnableTxObserver
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryTestConfiguration
import com.wavesenterprise.we.tx.observer.starter.observer.config.ObjectMapperConfig
import com.wavesplatform.we.flyway.schema.starter.FlywaySchemaConfiguration
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

private const val PROPERTY_TX_TYPE_1 = 1
private const val PROPERTY_TX_TYPE_2 = 3

@DataJpaTest(
    properties = [
        "tx-observer.predicate.tx-types = $PROPERTY_TX_TYPE_1,$PROPERTY_TX_TYPE_2"
    ]
)
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        ObjectMapperConfig::class,
        NodeBlockingServiceFactoryTestConfiguration::class,
        TxObserverEnabledNoConfigurerTest.Config::class,
        DataSourceAutoConfiguration::class,
        TxObserverStarterConfig::class,
        TxObserverJpaAutoConfig::class,
        FlywaySchemaConfiguration::class,
        TxObserverJpaConfig::class,
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TxObserverEnabledNoConfigurerTest {
    companion object {
        private val mockContextEnqueuePredicate: TxEnqueuePredicate = mockk()
    }

    @Autowired
    lateinit var partitionResolver: TxQueuePartitionResolver

    @Autowired
    lateinit var privateContentResolver: PrivateContentResolver

    @Autowired
    lateinit var enqueuePredicate: TxEnqueuePredicate

    @Test
    internal fun `should load default observer configurer adapter`() {
        val txPropertyType1: Tx = TestDataFactory.genesisTx()
        val txPropertyType2: Tx = TestDataFactory.issueTx()
        val txType1 = TestDataFactory.transferTx()
        val txType2 = TestDataFactory.reissueTx()
        assertTrue(partitionResolver is DefaultPartitionResolver)
        assertTrue(privateContentResolver is PrivateContentResolverImpl)
        assertTrue(enqueuePredicate.isEnqueued(txPropertyType1))
        assertTrue(enqueuePredicate.isEnqueued(txPropertyType2))
        assertFalse(enqueuePredicate.isEnqueued(txType1))
        assertFalse(enqueuePredicate.isEnqueued(txType2))
        verify(exactly = 0) { mockContextEnqueuePredicate.isEnqueued(any()) }
    }

    @Configuration
    @EnableTxObserver
    internal class Config {
        @Bean
        fun enqueuePredicate(): TxEnqueuePredicate =
            mockContextEnqueuePredicate
    }
}
