package com.wavesenterprise.sdk.tx.observer.core.spring.component

interface OffsetProvider {
    fun provideOffset(upperBound: Int): Int
}
