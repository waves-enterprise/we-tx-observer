package com.wavesenterprise.sdk.tx.observer.starter.observer.enabler

import com.wavesenterprise.sdk.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.sdk.node.domain.TxType
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.tx.observer.api.partition.TxQueuePartitionResolver
import com.wavesenterprise.sdk.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.sdk.tx.observer.api.tx.TxEnqueuePredicate
import com.wavesenterprise.sdk.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.sdk.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.sdk.tx.observer.starter.TxObserverConfigurer
import com.wavesenterprise.sdk.tx.observer.starter.TxObserverConfigurerBuilder
import com.wavesenterprise.sdk.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.sdk.tx.observer.starter.annotation.EnableTxObserver
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryTestConfiguration
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.ObjectMapperConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        ObjectMapperConfig::class,
        NodeBlockingServiceFactoryTestConfiguration::class,
        TxObserverEnabledConfigurerWithBuilderTest.Config::class,
        DataSourceAutoConfiguration::class,
        TxObserverStarterConfig::class,
        TxObserverJpaAutoConfig::class,
        FlywaySchemaConfiguration::class,
        TxObserverJpaConfig::class,
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TxObserverEnabledConfigurerWithBuilderTest {

    @Autowired
    lateinit var partitionResolver: TxQueuePartitionResolver

    @Autowired
    lateinit var privateContentResolver: PrivateContentResolver

    @Autowired
    lateinit var enqueuePredicate: TxEnqueuePredicate

    @Test
    internal fun `should configure beans using configurer`() {
        Assertions.assertEquals(partitionResolver, partitionResolver)
        Assertions.assertEquals(privateContentResolver, privateContentResolver)

        val txPropertyType1: Tx = TestDataFactory.genesisTx()
        Assertions.assertFalse(enqueuePredicate.isEnqueued(txPropertyType1))
        verify { enqueuePredicate.isEnqueued(txPropertyType1) }

        val txPropertyType2: Tx = TestDataFactory.issueTx()
        Assertions.assertFalse(enqueuePredicate.isEnqueued(txPropertyType2))
        verify { enqueuePredicate.isEnqueued(txPropertyType2) }

        val txType1: Tx = TestDataFactory.transferTx()
        Assertions.assertTrue(enqueuePredicate.isEnqueued(txType1))
        verify { enqueuePredicate.isEnqueued(txType1) }

        val txType2: Tx = TestDataFactory.reissueTx()
        Assertions.assertTrue(enqueuePredicate.isEnqueued(txType2))
        verify { enqueuePredicate.isEnqueued(txType2) }

        val txType3: Tx = TestDataFactory.burnTx()
        Assertions.assertTrue(enqueuePredicate.isEnqueued(txType3))
        verify { enqueuePredicate.isEnqueued(txType3) }

        val txType4: Tx = TestDataFactory.leaseTx()
        Assertions.assertTrue(enqueuePredicate.isEnqueued(txType4))
        verify { enqueuePredicate.isEnqueued(txType4) }

        val txType5: Tx = TestDataFactory.createContractTx()
        Assertions.assertFalse(enqueuePredicate.isEnqueued(txType5))
        verify { enqueuePredicate.isEnqueued(txType5) }
    }

    companion object {
        private val partitionResolver: TxQueuePartitionResolver = mockk()
        private val privateContentResolver: PrivateContentResolver = mockk()
        private val enqueuePredicate: TxEnqueuePredicate = mockk {
            every { isEnqueued(any()) } returns true
        }
        private val TX_TYPE_1 = TxType.fromInt(4)
        private val TX_TYPE_2 = TxType.fromInt(5)
        private val TX_TYPE_3 = TxType.fromInt(6)
        private val TX_TYPE_4 = TxType.fromInt(8)
        private val txTypes = listOf(TX_TYPE_1, TX_TYPE_2)
    }

    @Configuration
    @EnableTxObserver
    internal class Config {
        @Bean
        fun txObserverConfigurer(): TxObserverConfigurer =
            TxObserverConfigurerBuilder()
                .partitionResolver(Companion.partitionResolver)
                .privateContentResolver(Companion.privateContentResolver)
                .predicate(Companion.enqueuePredicate)
                .types(txTypes)
                .types(TX_TYPE_3, TX_TYPE_4)
                .build()
    }
}
