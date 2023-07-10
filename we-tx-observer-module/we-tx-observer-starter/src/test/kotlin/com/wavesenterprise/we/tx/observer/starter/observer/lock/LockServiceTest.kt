package com.wavesenterprise.we.tx.observer.starter.observer.lock

import com.wavesenterprise.we.tx.observer.common.tx.executor.TxExecutor
import com.wavesenterprise.we.tx.observer.core.spring.lock.LockService
import com.wavesenterprise.we.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.we.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.we.tx.observer.starter.lock.LockConfig
import com.wavesplatform.we.flyway.schema.starter.FlywaySchemaConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.util.concurrent.ForkJoinPool

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        DataSourceAutoConfiguration::class,
        TxObserverJpaConfig::class,
        TxObserverJpaAutoConfig::class,
        LockConfig::class,
        FlywaySchemaConfiguration::class,
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LockServiceTest {

    @Autowired
    lateinit var lockService: LockService

    @Autowired
    lateinit var txExecutor: TxExecutor

    @Test
    fun `should allow only one transaction to block`() {
        val lockKey = "someKey"
        val sleepTime: Long = 500
        val fjp = ForkJoinPool(2)
        val firstLocked = fjp.submit<Boolean> {
            txExecutor.required {
                val locked = lockService.lock(lockKey)
                Thread.sleep(sleepTime)
                locked
            }
        }
        val secondLocked = fjp.submit<Boolean> {
            txExecutor.required {
                val locked = lockService.lock(lockKey)
                Thread.sleep(sleepTime)
                locked
            }
        }
        var numberOfLocks = 0
        if (firstLocked.join()) ++numberOfLocks
        if (secondLocked.join()) ++numberOfLocks
        assertThat(numberOfLocks).isEqualTo(1)
    }
}
