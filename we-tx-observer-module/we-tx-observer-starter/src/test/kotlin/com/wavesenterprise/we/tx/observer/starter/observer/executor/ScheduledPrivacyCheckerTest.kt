package com.wavesenterprise.we.tx.observer.starter.observer.executor

import com.wavesenterprise.we.tx.observer.core.spring.executor.PrivacyAvailabilityChecker
import com.wavesenterprise.we.tx.observer.core.spring.executor.ScheduledPrivacyChecker
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class ScheduledPrivacyCheckerTest {

    @MockK
    lateinit var privacyAvailabilityChecker: PrivacyAvailabilityChecker

    @InjectMockKs
    lateinit var scheduledPrivacyAvailabilityChecker: ScheduledPrivacyChecker

    @Test
    fun `should check privacy availability until they are finished`() {
        every {
            privacyAvailabilityChecker.checkPrivacyAvailability()
        } returnsMany listOf(1L, 2L, 0L, 3L)

        scheduledPrivacyAvailabilityChecker.checkPrivacyAvailabilityWhileTheyExist()

        verify(exactly = 3) {
            privacyAvailabilityChecker.checkPrivacyAvailability()
        }
        confirmVerified(privacyAvailabilityChecker)
    }
}
