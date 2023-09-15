package com.wavesenterprise.we.tx.observer.starter.observer.enabler

import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.we.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.we.tx.observer.api.partition.TxQueuePartitionResolver
import com.wavesenterprise.we.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.we.tx.observer.api.tx.TxEnqueuePredicate
import com.wavesenterprise.we.tx.observer.core.spring.method.callback.PrivateContentResolverImpl
import com.wavesenterprise.we.tx.observer.core.spring.partition.DefaultPartitionResolver
import com.wavesenterprise.we.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.we.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.we.tx.observer.starter.TxObserverConfigurer
import com.wavesenterprise.we.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryTestConfiguration
import com.wavesenterprise.we.tx.observer.starter.observer.config.ObjectMapperConfig
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
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
        DataSourceAutoConfiguration::class,
        TxObserverStarterConfig::class,
        TxObserverJpaAutoConfig::class,
        FlywaySchemaConfiguration::class,
        TxObserverJpaConfig::class,
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TxObserverNoConfigurerContextTest {
    @Autowired(required = false)
    var txObserverConfigurer: TxObserverConfigurer? = null

    @Autowired
    lateinit var partitionResolver: TxQueuePartitionResolver

    @Autowired
    lateinit var privateContentResolver: PrivateContentResolver

    @Autowired
    lateinit var enqueuePredicate: TxEnqueuePredicate

    @Test
    internal fun `should configure beans not using configurer`() {
        assertNull(txObserverConfigurer)
        assertTrue(partitionResolver is DefaultPartitionResolver)
        assertTrue(privateContentResolver is PrivateContentResolverImpl)
        assertTrue(enqueuePredicate.isEnqueued(TestDataFactory.genesisTx()))
        assertTrue(enqueuePredicate.isEnqueued(TestDataFactory.issueTx()))
        assertFalse(enqueuePredicate.isEnqueued(TestDataFactory.transferTx()))
        assertFalse(enqueuePredicate.isEnqueued(TestDataFactory.reissueTx()))
    }
}
