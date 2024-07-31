package com.wavesenterprise.sdk.tx.observer.starter.observer.jpa

import com.wavesenterprise.sdk.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.sdk.tx.observer.common.tx.executor.TxExecutor
import com.wavesenterprise.sdk.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.sdk.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.sdk.tx.observer.jpa.repository.BlockHistoryRepository
import com.wavesenterprise.sdk.tx.observer.starter.observer.util.ModelFactory.blockHistory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        DataSourceAutoConfiguration::class,
        TxObserverJpaConfig::class,
        TxObserverJpaAutoConfig::class,
        FlywaySchemaConfiguration::class,
    ],
)
@EntityScan("com.wavesenterprise.sdk.tx.observer")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BlockHistoryRepositoryTest {

    @Autowired
    lateinit var blockHistoryRepository: BlockHistoryRepository

    @Autowired
    lateinit var txExecutor: TxExecutor

    @Test
    fun `should delete blocks before given height`() {
        val threshold = 100L
        blockHistoryRepository.saveAll(
            (90L until threshold).map { height -> blockHistory(height = height) },
        )
        val blocksToIgnore = blockHistoryRepository.saveAll(
            (threshold..110L).map { height -> blockHistory(height = height) },
        )

        blockHistoryRepository.deleteAllByHeightBefore(threshold)

        val blockHistoriesFromRepo = blockHistoryRepository.findAll()
        assertThat(blockHistoriesFromRepo).containsExactlyInAnyOrderElementsOf(blocksToIgnore)
    }

    @Test
    fun `should not fail on unique constraint when two block history with the same signature are saved`() {
        val signature = "iWi9XLIn"
        val expectedSavedEntity = txExecutor.requiresNew {
            val first = blockHistoryRepository.save(blockHistory(signature = signature))
            blockHistoryRepository.save(blockHistory(signature = signature))
            first
        }
        assertThat(blockHistoryRepository.findAll()).containsOnly(expectedSavedEntity)
    }
}
