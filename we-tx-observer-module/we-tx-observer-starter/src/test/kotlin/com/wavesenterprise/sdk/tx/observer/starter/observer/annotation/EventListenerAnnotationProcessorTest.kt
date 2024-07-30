package com.wavesenterprise.sdk.tx.observer.starter.observer.annotation

import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import com.wavesenterprise.sdk.tx.observer.common.tx.subscriber.TxSubscriber
import com.wavesenterprise.sdk.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.BlockListenerTestContextConfiguration
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration
import com.wavesenterprise.sdk.tx.observer.starter.observer.sample.TestBlockListeners
import com.wavesenterprise.sdk.tx.observer.starter.observer.sample.TestEventListeners
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    classes = [
        TxObserverStarterConfig::class,
        BlockListenerTestContextConfiguration::class,
        NodeBlockingServiceFactoryMockConfiguration::class,
    ],
)
// todo implement test slice
@ActiveProfiles("test")
@Disabled
internal class EventListenerAnnotationProcessorTest {

    @MockBean(name = "testEventListenersMock")
    lateinit var testEventListeners: TestEventListeners

//    @MockBean
//    lateinit var blockHeightRepository: BlockHeightJpaRepository

    @Autowired
    lateinit var txSubscribers: List<TxSubscriber>

    val txListenerAnnotatedMethods = TestBlockListeners::class.java.methods
        .filter { it.getAnnotation(TxListener::class.java) != null }
        .toList()

    @Test
    fun testSubscriberCreatedInApplicationContext() {
        assertEquals(txListenerAnnotatedMethods.size, txSubscribers.size)
    }
}
