package com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber

import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.event.BlockchainEvent
import com.wavesenterprise.sdk.tx.observer.api.block.WeRollbackInfo
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber.strategy.Action.HandleRollback
import org.slf4j.lazyLogger
import org.slf4j.warn

class HandleRollbackFactoryImpl(
    private val blocksService: BlocksService,
) : HandleRollbackFactory {
    private val log by lazyLogger(HandleRollbackFactory::class)

    override fun create(event: BlockchainEvent.RollbackCompleted): HandleRollback {
        var height: Long? = null
        return try {
            height = blocksService.blockById(event.returnToBlockSignature).height.value
            HandleRollback(
                event.toWeRollbackInfo(
                    toHeight = Height(height),
                ),
            )
        } finally {
            log.warn { "Rollback event signature = ${event.returnToBlockSignature}, height: $height" }
        }
    }

    companion object {
        private class ManageRollbackWeRollbackInfo(
            private val event: BlockchainEvent.RollbackCompleted,
            override val toHeight: Height,
        ) : WeRollbackInfo {
            override val toBlockSignature: Signature
                get() = event.returnToBlockSignature
            override val rollbackTxIds: Collection<TxId>
                get() = event.rollbackTxIds
        }

        private fun BlockchainEvent.RollbackCompleted.toWeRollbackInfo(toHeight: Height): WeRollbackInfo =
            ManageRollbackWeRollbackInfo(this, toHeight)
    }
}
