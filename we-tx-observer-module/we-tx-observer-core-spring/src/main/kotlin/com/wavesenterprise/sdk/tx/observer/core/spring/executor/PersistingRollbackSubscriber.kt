package com.wavesenterprise.sdk.tx.observer.core.spring.executor

import com.wavesenterprise.sdk.tx.observer.api.block.WeRollbackInfo
import com.wavesenterprise.sdk.tx.observer.api.block.subscriber.RollbackSubscriber
import com.wavesenterprise.sdk.tx.observer.domain.RollbackInfo
import com.wavesenterprise.sdk.tx.observer.jpa.repository.RollbackInfoRepository

class PersistingRollbackSubscriber(
    private val rollbackInfoRepository: RollbackInfoRepository,
) : RollbackSubscriber {
    override fun onRollback(weRollbackInfo: WeRollbackInfo) {
        rollbackInfoRepository.save(
            RollbackInfo(
                toHeight = weRollbackInfo.toHeight.value,
                toBlockSignature = weRollbackInfo.toBlockSignature.asBase58String(),
            ),
        )
    }
}
