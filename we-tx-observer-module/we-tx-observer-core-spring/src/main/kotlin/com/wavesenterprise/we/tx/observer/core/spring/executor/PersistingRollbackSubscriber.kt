package com.wavesenterprise.we.tx.observer.core.spring.executor

import com.wavesenterprise.we.tx.observer.api.block.WeRollbackInfo
import com.wavesenterprise.we.tx.observer.api.block.subscriber.RollbackSubscriber
import com.wavesenterprise.we.tx.observer.domain.RollbackInfo
import com.wavesenterprise.we.tx.observer.jpa.repository.RollbackInfoRepository

class PersistingRollbackSubscriber(
    private val rollbackInfoRepository: RollbackInfoRepository,
) : RollbackSubscriber {
    override fun onRollback(weRollbackInfo: WeRollbackInfo) {
        rollbackInfoRepository.save(
            RollbackInfo(
                toHeight = weRollbackInfo.toHeight.value,
                toBlockSignature = weRollbackInfo.toBlockSignature.asBase58String()
            )
        )
    }
}
