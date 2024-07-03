package com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo

import jakarta.persistence.OptimisticLockException

class ExpectedHeightMismatchException(
    expectedCurrentHeight: Long,
) : OptimisticLockException(
    "Expected to update a row, but updated none. Expected currentHeight $expectedCurrentHeight",
)
