package com.wavesenterprise.we.tx.observer.starter.observer.listener.privacydataevent

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.privacy.PolicyItemRequest
import com.wavesenterprise.sdk.node.domain.tx.TxInfo
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.privacyInfoResponse
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.sampleCreatePolicyTx
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.samplePolicyDataHashTx
import com.wavesenterprise.we.tx.observer.starter.observer.listener.AbstractListenerTest
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import java.util.Optional

abstract class AbstractPrivateEventBlockListenerTest : AbstractListenerTest() {

    @BeforeEach
    fun initMocks() {
        every {
            nodeBlockingServiceFactory.txService()
        } returns txService
        every {
            nodeBlockingServiceFactory.addressService()
        } returns addressService
        every {
            nodeBlockingServiceFactory.blockchainEventsService()
        } returns blockchainEventsService
        every {
            nodeBlockingServiceFactory.blocksService()
        } returns blocksService
        every {
            nodeBlockingServiceFactory.contractService()
        } returns contractService
        every {
            nodeBlockingServiceFactory.nodeInfoService()
        } returns nodeInfoService
        every {
            nodeBlockingServiceFactory.privacyService()
        } returns privacyService

        every {
            txService.txInfo(txId = samplePolicyDataHashTx.policyId.txId)
        } returns Optional.of(
            TxInfo(
                height = Height(1),
                tx = sampleCreatePolicyTx,
            )
        )

        every {
            privacyService.info(
                request = PolicyItemRequest(
                    policyId = samplePolicyDataHashTx.policyId,
                    dataHash = samplePolicyDataHashTx.dataHash,
                )
            )
        } returns Optional.of(privacyInfoResponse)
    }
}
