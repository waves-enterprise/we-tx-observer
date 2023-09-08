package com.wavesenterprise.we.tx.tracker.domain

import com.fasterxml.jackson.databind.node.TextNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class SmartContractInfoTest {

    @Test
    fun `should not produce StackOverflow with SmartContractInfo when invoking toString`() {
        val smartContractInfo = SmartContractInfo(
            id = "1",
            imageHash = "imgHash",
            image = "image",
            version = 1,
            contractName = "bla",
            sender = "123"
        )
        val txTrackInfo = TxTrackInfo(
            id = "id1",
            status = TxTrackStatus.SUCCESS,
            type = 1,
            body = TextNode("ff"),
            meta = mapOf(),
            smartContractInfo = smartContractInfo
        )
        smartContractInfo.txTrackInfos += txTrackInfo
        assertDoesNotThrow {
            smartContractInfo.toString()
        }
    }
}
