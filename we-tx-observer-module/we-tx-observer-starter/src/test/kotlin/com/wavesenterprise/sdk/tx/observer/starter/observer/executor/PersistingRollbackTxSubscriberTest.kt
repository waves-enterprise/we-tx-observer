package com.wavesenterprise.sdk.tx.observer.starter.observer.executor

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.tx.observer.api.block.WeRollbackInfo
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.PersistingRollbackSubscriber
import com.wavesenterprise.sdk.tx.observer.domain.RollbackInfo
import com.wavesenterprise.sdk.tx.observer.jpa.repository.RollbackInfoRepository
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class PersistingRollbackTxSubscriberTest {
    @MockK
    private lateinit var rollbackInfoRepository: RollbackInfoRepository

    @InjectMockKs
    private lateinit var persistingRollbackSubscriber: PersistingRollbackSubscriber

    @Test
    fun `should save rollback info`() {
        val height = 101L
        val signature = "tU47Pa4"
        val weRollbackInfo: WeRollbackInfo = mockk {
            every { toHeight } returns Height(height)
            every { toBlockSignature } returns Signature.fromBase58(signature)
        }
        every { rollbackInfoRepository.save(any()) } answers { arg(0) }

        persistingRollbackSubscriber.onRollback(weRollbackInfo)

        verifyOrder {
            weRollbackInfo.toHeight
            weRollbackInfo.toBlockSignature
            rollbackInfoRepository.save(
                RollbackInfo(
                    toHeight = height,
                    toBlockSignature = signature
                )
            )
        }
        confirmVerified(weRollbackInfo, rollbackInfoRepository)
    }
}
