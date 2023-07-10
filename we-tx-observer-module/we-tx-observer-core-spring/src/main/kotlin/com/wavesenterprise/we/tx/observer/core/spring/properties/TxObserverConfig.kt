package com.wavesenterprise.we.tx.observer.core.spring.properties

import org.springframework.util.unit.DataSize
import java.time.Duration

interface TxObserverConfig {
    var enabled: Boolean
    var queueMode: String
    var fixedDelay: Duration
    var blockSizeWindow: DataSize
    var activationHeight: Long
    var nodeAliasForHeight: String
    var blockHeightWindow: Long
    var syncHistory: Boolean
    var blockHistoryDepth: Int
    var forkNotResolvedHeightDrop: Long
    var blockHistoryCleanDelay: Duration
    var liquidBlockPollingDelay: Long
    var autoResetHeight: Boolean
    var errorPriorityOffset: Int
    var defaultPartitionId: String
    var lockEnabled: Boolean
    var lockAtLeast: Long
    var lockAtMost: Long
}
