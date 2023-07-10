package com.wavesenterprise.we.tx.observer.core.spring.component

interface OffsetProvider {
    fun provideOffset(upperBound: Int): Int
}
