package com.wavesenterprise.we.tx.observer.core.spring.component

import kotlin.random.Random

class RandomOffsetProvider : OffsetProvider {

    override fun provideOffset(upperBound: Int): Int = if (upperBound <= 0) 0 else Random.nextInt(0, upperBound)
}
