package com.wavesenterprise.we.tx.observer.starter.properties

import com.wavesenterprise.we.tx.observer.core.spring.properties.TxObserverConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.boot.convert.DataSizeUnit
import org.springframework.boot.convert.DurationUnit
import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit
import java.time.Duration
import java.time.temporal.ChronoUnit

@ConfigurationProperties("tx-observer")
@ConstructorBinding
data class TxObserverProperties(
    @DefaultValue("true")
    override var enabled: Boolean,
    override var queueMode: String = "JPA",
    @DurationUnit(ChronoUnit.MILLIS) @DefaultValue("2s")
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
    override var lockEnabled: Boolean = true,
    override var lockAtLeast: Long = 0,
    override var lockAtMost: Long = 10000,
    // todo: object mapper bean ref if exist
) : TxObserverConfig
