package com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber

import com.wavesenterprise.sdk.node.client.blocking.event.BlockchainEventsIterator
import com.wavesenterprise.sdk.node.client.blocking.event.BlockchainEventsService
import com.wavesenterprise.sdk.node.client.blocking.event.fromBlock
import com.wavesenterprise.sdk.node.client.blocking.event.fromGenesis
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.exception.NodeServiceUnavailableException
import com.wavesenterprise.we.tx.observer.api.block.subscriber.BlockSubscriber
import com.wavesenterprise.we.tx.observer.api.block.subscriber.RollbackSubscriber
import com.wavesenterprise.we.tx.observer.common.tx.executor.TxExecutor
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.strategy.Action
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.strategy.Action.HandleBlocks
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.strategy.Action.HandleRollback
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.strategy.EventHandlingStrategy
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.strategy.EventHandlingStrategyFactory
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.ExpectedHeightMismatchException
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.FIRST_BLOCK_HEIGHT
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncedBlockInfo
import com.wavesenterprise.we.tx.observer.core.spring.lock.LockService
import org.slf4j.debug
import org.slf4j.error
import org.slf4j.info
import org.slf4j.lazyLogger
import org.slf4j.warn

class EventSubscriber(
    private val syncInfoService: SyncInfoService,
    private val weBlockchainEventServices: List<BlockchainEventsService>,
    private val eventHandlingStrategyFactory: EventHandlingStrategyFactory,
    private val blockSubscribers: Collection<BlockSubscriber>,
    private val rollbackSubscribers: Collection<RollbackSubscriber>,
    private val txExecutor: TxExecutor,
    private val lockService: LockService,
) {
    private val log by lazyLogger(EventSubscriber::class)

    init {
        if (weBlockchainEventServices.isEmpty()) error("No WeBlockchainEventServices provided")
    }

    private var currentWeBlockchainEventServiceIndex: Int = 0
    private val weBlockchainEventService: BlockchainEventsService
        get() = weBlockchainEventServices[currentWeBlockchainEventServiceIndex]
    private val syncState = SyncState()

    /**
     * Tries to obtain lock for multi-instance setup and synchronize events
     */
    fun subscribe() {
        try {
            txExecutor.required {
                val locked: Boolean = lockService.lock(EVENT_SUBSCRIBER_LOCK_NAME)
                if (!locked) {
                    log.info { "Lock not acquired" }
                } else {
                    log.info { "Lock acquired" }
                    syncEvents()
                }
            }
        } catch (ex: NodeServiceUnavailableException) {
            log.error(ex) { "Rotating node" }
            rotateWeBlockchainEventService()
        } catch (ex: ExpectedHeightMismatchException) {
            log.warn { "Observer height was updated. Trying to take lock and sync again" }
        } catch (ex: Exception) {
            log.error(ex) { "Error in event subscriber" }
        }
    }

    /**
     * Sync events from current block signature obtained from syncInfoService.
     * while(true) loop is used to handle recoverable errors
     */
    private fun syncEvents() {
        var forkDetected = false
        while (true) {
            try {
                val syncInfo = txExecutor.requiresNew {
                    if (forkDetected)
                        syncInfoService.syncInfoOnFork().also {
                            forkDetected = false
                        }
                    else
                        syncInfoService.syncInfo()
                }
                with(syncInfo) {
                    syncState.change(
                        height = observerHeight.value,
                        prevBlockSignature = prevBlockSignature?.asBase58String()
                    )
                }
                with(syncState) {
                    when (val prevBlockSignature = prevBlockSignature) {
                        null -> weBlockchainEventService.fromGenesis().also {
                            log.info { "Subscribed from genesis" }
                        }
                        else -> weBlockchainEventService.fromBlock(Signature.fromBase58(prevBlockSignature)).also {
                            log.info { "Subscribed from signature $prevBlockSignature, height $height" }
                        }
                    }.use { events ->
                        handleEvents(
                            events = events,
                            strategy = eventHandlingStrategyFactory.create(height = height)
                        )
                    }
                }
            } catch (ex: Exception) { // todo when impl blockchainEventService
                log.warn(ex) { "Fork detected. Trying to resume subscription" }
                forkDetected = true
            }
        }
    }

    private fun handleEvents(
        events: BlockchainEventsIterator,
        strategy: EventHandlingStrategy
    ) {
        for (event in events) {
            log.debug { "Received event $event" }
            strategy
                .actionsOn(event)
                .forEach(::handleAction)
        }
    }

    private fun handleAction(action: Action) {
        when (action) {
            is HandleBlocks -> with(action) {
                txExecutor.requiresNew {
                    weBlockInfos.forEach { weBlockInfo ->
                        blockSubscribers.forEach { subscriber ->
                            subscriber.subscribe(weBlockInfo)
                        }
                    }
                    val lastSyncedBlockInfo = syncedBlockInfos.lastOrNull()
                    if (lastSyncedBlockInfo != null)
                        syncedTo(
                            height = lastSyncedBlockInfo.height.value + 1,
                            prevBlockSignature = lastSyncedBlockInfo.signature.asBase58String(),
                            syncedBlocks = syncedBlockInfos
                        )
                }
            }
            is HandleRollback -> with(action) {
                txExecutor.requiresNew {
                    rollbackSubscribers.forEach { subscriber ->
                        subscriber.onRollback(weRollbackInfo)
                    }
                    syncedTo(
                        height = weRollbackInfo.toHeight.value + 1,
                        prevBlockSignature = weRollbackInfo.toBlockSignature.asBase58String()
                    )
                }
            }
        }
    }

    private fun syncedTo(
        height: Long,
        prevBlockSignature: String,
        syncedBlocks: List<SyncedBlockInfo> = emptyList()
    ) {
        syncInfoService.syncedTo(
            height = height,
            prevBlockSignature = prevBlockSignature,
            expectedCurrentHeight = syncState.height,
            syncedBlocks = syncedBlocks
        )
        syncState.change(
            height = height,
            prevBlockSignature = prevBlockSignature
        )
    }

    private fun rotateWeBlockchainEventService() {
        currentWeBlockchainEventServiceIndex = when (currentWeBlockchainEventServiceIndex) {
            weBlockchainEventServices.lastIndex -> 0
            else -> currentWeBlockchainEventServiceIndex + 1
        }
    }

    private class SyncState {
        var height: Long = FIRST_BLOCK_HEIGHT
            private set
        var prevBlockSignature: String? = null
            private set

        fun change(height: Long, prevBlockSignature: String?) {
            this.height = height
            this.prevBlockSignature = prevBlockSignature
        }
    }

    companion object {
        private const val EVENT_SUBSCRIBER_LOCK_NAME = "EVENT_SUBSCRIBER"
    }
}
