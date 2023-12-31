package com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo

import javax.persistence.OptimisticLockException

class ExpectedHeightMismatchException(
    expectedCurrentHeight: Long
) : OptimisticLockException(
    "Expected to update a row, but updated none. Expected currentHeight $expectedCurrentHeight"
)
