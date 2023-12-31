package com.wavesenterprise.we.tx.tracker

import com.wavesenterprise.we.tx.tracker.api.TxTrackInfoService
import com.wavesenterprise.we.tx.tracker.read.starter.TxTrackerReadJpaRepository
import com.wavesenterprise.we.tx.tracker.read.starter.web.TxTrackInfoController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    properties = ["tx-tracker.enabled = true"]
)
@ActiveProfiles("test")
class TxTrackerReadAutoConfigTest(
    private val applicationContext: ApplicationContext
) {
    @Test
    fun `context loads`() {
    }

    @Test
    fun `context has all beans`() {
        applicationContext.getBean<TxTrackInfoController>()
        applicationContext.getBean<TxTrackInfoService>()
        applicationContext.getBean<TxTrackerReadJpaRepository>()
    }

    @SpringBootApplication
    class TxTrackerReadTestApp
}
