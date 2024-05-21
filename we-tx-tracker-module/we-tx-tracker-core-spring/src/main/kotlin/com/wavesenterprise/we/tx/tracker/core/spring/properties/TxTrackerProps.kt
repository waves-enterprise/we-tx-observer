package com.wavesenterprise.we.tx.tracker.core.spring.properties

import java.time.Duration

interface TxTrackerProps {
    var enabled: Boolean
    var findContractInNode: Boolean
    var fixedDelay: Duration
    var trackedTxPageRequestLimit: Int
    var timeout: Duration
    var minContractTxErrorCount: Int
    var minContractTxFailureCount: Int
    var failOnRecoverableContractTxError: Boolean
}
