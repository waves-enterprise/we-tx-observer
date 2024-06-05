package com.wavesenterprise.sdk.tx.observer.api

open class BlockListenerException(message: String, cause: Throwable) : RuntimeException(message, cause)

class BlockListenerHandlerException(message: String, cause: Throwable) : BlockListenerException(message, cause)

class BlockListenerSingleTxHandlerException(
    txId: String,
    message: String,
    cause: Throwable,
) : BlockListenerException(message, cause)

class NoPayloadException(
    message: String = "No payload",
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class PartitionHandlingException(val partitionId: String, cause: Throwable) : RuntimeException(cause)
