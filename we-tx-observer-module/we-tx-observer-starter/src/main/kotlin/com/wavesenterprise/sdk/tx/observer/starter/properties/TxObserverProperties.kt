package com.wavesenterprise.sdk.tx.observer.starter.properties

import com.wavesenterprise.sdk.tx.observer.core.spring.properties.TxObserverConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.boot.convert.DataSizeUnit
import org.springframework.boot.convert.DurationUnit
import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit
import java.time.Duration
import java.time.temporal.ChronoUnit

@ConfigurationProperties("tx-observer")
data class TxObserverProperties(
    @DefaultValue("true")
    override var enabled: Boolean,
    override var queueMode: String = "JPA",
    @DefaultValue("50ms")
    override var fixedDelay: Duration,
    @DataSizeUnit(DataUnit.MEGABYTES)
    override var blockSizeWindow: DataSize = DataSize.ofMegabytes(10),
    override var activationHeight: Long = 1,
    override var blockHeightWindow: Long = 99,
    override var syncHistory: Boolean = true,
    override var pauseSyncAtQueueSize: Long = 10000,
    override var blockHistoryDepth: Int = 100,
    override var forkNotResolvedHeightDrop: Long = 10,
    @DurationUnit(ChronoUnit.MILLIS)
    override var blockHistoryCleanDelay: Duration = Duration.ofMinutes(30),
    override var liquidBlockPollingDelay: Long = 200,
    override var autoResetHeight: Boolean = false,
    override var errorPriorityOffset: Int = 100,
    override var defaultPartitionId: String = "defaultPartitionId",
    @DefaultValue("true")
    override var lockEnabled: Boolean,
    // todo: object mapper bean ref if exist
) : TxObserverConfig
