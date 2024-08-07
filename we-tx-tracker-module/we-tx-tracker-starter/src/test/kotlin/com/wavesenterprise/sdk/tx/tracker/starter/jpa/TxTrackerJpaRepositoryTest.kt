package com.wavesenterprise.sdk.tx.tracker.starter.jpa

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import com.wavesenterprise.sdk.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.sdk.node.client.blocking.tx.TxService
import com.wavesenterprise.sdk.node.domain.TxType
import com.wavesenterprise.sdk.tx.observer.common.jpa.util.flushAndClear
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackInfo
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackStatus
import com.wavesenterprise.sdk.tx.tracker.jpa.TxTrackerJpaAutoConfig
import com.wavesenterprise.sdk.tx.tracker.jpa.repository.TxTrackerJpaRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.PersistenceException
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.util.UUID

@DataJpaTest(properties = ["tx-tracker.enabled = true"])
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        DataSourceAutoConfiguration::class,
        TxTrackerJpaAutoConfig::class,
        FlywaySchemaConfiguration::class,
    ],
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TxTrackerJpaRepositoryTest {

    @PersistenceContext
    lateinit var em: EntityManager

    @Autowired
    lateinit var txTrackerJpaRepository: TxTrackerJpaRepository

    @MockkBean
    lateinit var txService: TxService

    private val objectMapper = jacksonObjectMapper()

    @ParameterizedTest
    @EnumSource(
        value = TxType::class,
        names = ["CREATE_CONTRACT", "CALL_CONTRACT", "DISABLE_CONTRACT", "UPDATE_CONTRACT"],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `should throw sql constraint violation when tx type is one of contract modification`(type: TxType) {
        assertThrows<PersistenceException> {
            txTrackerJpaRepository.save(buildTxTrackInfo(type = type))
            em.flushAndClear()
        }.apply {
            assertThat((this as ConstraintViolationException).constraintName, `is`("contract_id_null_check"))
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = TxType::class,
        names = ["CREATE_CONTRACT", "CALL_CONTRACT", "DISABLE_CONTRACT", "UPDATE_CONTRACT"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `should doesn't throw exception when tx type is not contract modification`(type: TxType) {
        txTrackerJpaRepository.save(buildTxTrackInfo(type = type))
        em.flushAndClear()
    }

    private fun buildTxTrackInfo(type: TxType) = TxTrackInfo(
        id = UUID.randomUUID().toString(),
        status = TxTrackStatus.PENDING,
        smartContractInfo = null,
        type = type.code,
        body = objectMapper.valueToTree(mapOf<String, String>()),
        meta = mapOf(),
        userId = "USER",
    )
}
