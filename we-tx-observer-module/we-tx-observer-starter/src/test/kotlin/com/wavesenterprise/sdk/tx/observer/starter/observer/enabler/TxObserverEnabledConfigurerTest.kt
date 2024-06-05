package com.wavesenterprise.sdk.tx.observer.starter.observer.enabler

import com.wavesenterprise.sdk.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.tx.observer.api.partition.TxQueuePartitionResolver
import com.wavesenterprise.sdk.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.sdk.tx.observer.api.tx.TxEnqueuePredicate
import com.wavesenterprise.sdk.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.sdk.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.sdk.tx.observer.starter.TxObserverConfigurer
import com.wavesenterprise.sdk.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.sdk.tx.observer.starter.annotation.EnableTxObserver
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryTestConfiguration
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.ObjectMapperConfig
import com.wavesenterprise.sdk.tx.observer.starter.observerConfigurer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
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
        TxObserverEnabledConfigurerTest.Config::class,
        DataSourceAutoConfiguration::class,
        TxObserverStarterConfig::class,
        TxObserverJpaAutoConfig::class,
        FlywaySchemaConfiguration::class,
        TxObserverJpaConfig::class,
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TxObserverEnabledConfigurerTest {
    companion object {
        private val partitionResolver: TxQueuePartitionResolver = mockk()
        private val privateContentResolver: PrivateContentResolver = mockk()
        private val enqueuePredicate: TxEnqueuePredicate = mockk {
            every { isEnqueued(any()) } returns true
        }
        private const val TX_TYPE_1 = 4
        private const val TX_TYPE_2 = 5
        private const val TX_TYPE_3 = 6
        private const val TX_TYPE_4 = 8
        private val txTypes = listOf(TX_TYPE_1, TX_TYPE_2)
    }

    @Autowired
    lateinit var partitionResolver: TxQueuePartitionResolver

    @Autowired
    lateinit var privateContentResolver: PrivateContentResolver

    @Autowired
    lateinit var enqueuePredicate: TxEnqueuePredicate

    @Test
    internal fun `should configure beans using configurer`() {
        assertEquals(Companion.partitionResolver, partitionResolver)
        assertEquals(Companion.privateContentResolver, privateContentResolver)

        val txPropertyType1: Tx = TestDataFactory.genesisTx()
        assertFalse(enqueuePredicate.isEnqueued(txPropertyType1))
        verify { Companion.enqueuePredicate.isEnqueued(txPropertyType1) }

        val txPropertyType2: Tx = TestDataFactory.issueTx()
        assertFalse(enqueuePredicate.isEnqueued(txPropertyType2))
        verify { Companion.enqueuePredicate.isEnqueued(txPropertyType2) }

        val txType1: Tx = TestDataFactory.transferTx()
        assertTrue(enqueuePredicate.isEnqueued(txType1))
        verify { Companion.enqueuePredicate.isEnqueued(txType1) }

        val txType2: Tx = TestDataFactory.reissueTx()
        assertTrue(enqueuePredicate.isEnqueued(txType2))
        verify { Companion.enqueuePredicate.isEnqueued(txType2) }

        val txType3: Tx = TestDataFactory.burnTx()
        assertTrue(enqueuePredicate.isEnqueued(txType3))
        verify { Companion.enqueuePredicate.isEnqueued(txType3) }

        val txType4: Tx = TestDataFactory.leaseTx()
        assertTrue(enqueuePredicate.isEnqueued(txType4))
        verify { Companion.enqueuePredicate.isEnqueued(txType4) }

        val txType5: Tx = TestDataFactory.createContractTx()
        assertFalse(enqueuePredicate.isEnqueued(txType5))
        verify { Companion.enqueuePredicate.isEnqueued(txType5) }
    }

    @Configuration
    @EnableTxObserver
    internal class Config {
        @Bean
        fun txObserverConfigurer(): TxObserverConfigurer = observerConfigurer {
            partitionResolver = Companion.partitionResolver
            privateContentResolver = Companion.privateContentResolver
            predicates {
                types(txTypes)
                types(TX_TYPE_3, TX_TYPE_4)
                predicate(enqueuePredicate)
            }
        }
    }
}
