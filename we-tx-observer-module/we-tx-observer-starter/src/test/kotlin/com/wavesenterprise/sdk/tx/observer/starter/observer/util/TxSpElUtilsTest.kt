package com.wavesenterprise.sdk.tx.observer.starter.observer.util

import com.wavesenterprise.sdk.node.domain.DataEntry
import com.wavesenterprise.sdk.node.domain.DataKey
import com.wavesenterprise.sdk.node.domain.DataValue
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.tx.observer.core.spring.util.TxSpElUtils.Companion.hasKeyWithPrefix
import com.wavesenterprise.sdk.tx.observer.core.spring.util.TxSpElUtils.Companion.hasKeyWithRegex
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TxSpElUtilsTest {

    private val sampleTx = TestDataFactory.executedContractTx(
        results = listOf(
            DataEntry(
                key = DataKey("key1_adfdf"),
                value = DataValue.StringDataValue("fffff"),
            ),
            DataEntry(
                key = DataKey("key2_aaaaa"),
                value = DataValue.StringDataValue("fffff"),
            ),
            DataEntry(
                key = DataKey("key3_aaaaaadfa"),
                value = DataValue.StringDataValue("fffff"),
            ),
        ),
        tx = TestDataFactory.createContractTx(
            params = listOf(
                DataEntry(
                    key = DataKey("key"),
                    value = DataValue.StringDataValue("fffff"),
                ),
            ),
        ),
    )

    @Test
    fun testHasKeyWithPrefix() {
        assertTrue(hasKeyWithPrefix(sampleTx, "key1"))
        assertTrue(hasKeyWithPrefix(sampleTx, "key2"))
        assertFalse(hasKeyWithPrefix(sampleTx, "key4"))
    }

    @Test
    fun testHasKeyWithRegex() {
        assertTrue(hasKeyWithRegex(sampleTx, "key1_a.*"))
        assertTrue(hasKeyWithRegex(sampleTx, "key1_.*"))
        assertFalse(hasKeyWithRegex(sampleTx, "key1__.*"))
    }
}
