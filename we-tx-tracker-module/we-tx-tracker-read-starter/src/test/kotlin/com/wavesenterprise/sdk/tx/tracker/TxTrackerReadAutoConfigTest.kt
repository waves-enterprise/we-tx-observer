package com.wavesenterprise.sdk.tx.tracker

import com.wavesenterprise.sdk.tx.tracker.api.TxTrackInfoService
import com.wavesenterprise.sdk.tx.tracker.read.starter.TxTrackerReadJpaRepository
import com.wavesenterprise.sdk.tx.tracker.read.starter.web.TxTrackInfoController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    properties = ["tx-tracker.enabled = true"]
)
@ActiveProfiles("test")
class TxTrackerReadAutoConfigTest {

    @Autowired
    lateinit var applicationContext: ApplicationContext

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
