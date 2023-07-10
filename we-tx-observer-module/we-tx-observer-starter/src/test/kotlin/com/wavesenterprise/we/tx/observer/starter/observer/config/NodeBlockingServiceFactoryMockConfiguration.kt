package com.wavesenterprise.we.tx.observer.starter.observer.config

import com.wavesenterprise.sdk.node.client.blocking.address.AddressService
import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.sdk.node.client.blocking.contract.ContractService
import com.wavesenterprise.sdk.node.client.blocking.event.BlockchainEventsService
import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.node.client.blocking.node.NodeInfoService
import com.wavesenterprise.sdk.node.client.blocking.privacy.PrivacyService
import com.wavesenterprise.sdk.node.client.blocking.tx.TxService
import com.wavesenterprise.sdk.node.client.blocking.util.UtilsService
import com.wavesenterprise.sdk.node.domain.DataEntry
import com.wavesenterprise.sdk.node.domain.DataKey
import com.wavesenterprise.sdk.node.domain.DataValue
import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.atomic.AtomicBadge
import com.wavesenterprise.sdk.node.domain.contract.ContractImage
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.we.tx.observer.domain.TxQueuePartition
import com.wavesenterprise.we.tx.observer.starter.observer.util.ModelFactory.blockAtHeight
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
class NodeBlockingServiceFactoryMockConfiguration {

    @Bean
    fun nodeBlockingServiceFactory(): NodeBlockingServiceFactory = mockk<NodeBlockingServiceFactory>().also {
        every { it.blocksService() } returns blocksService()
        every { it.txService() } returns txService()
        every { it.privacyService() } returns privacyService()
        every { it.contractService() } returns contractService()
        every { it.nodeInfoService() } returns nodeInfoService()
        every { it.blockchainEventsService() } returns blockchainEventsService()
        every { it.addressService() } returns addressService()
        every { it.utilService() } returns utilsService()
    }

    @Bean
    fun blocksService(): BlocksService = mockk<BlocksService>(relaxed = true).also {
        every { it.blockHeight() } returns Height(1)
        every { it.blockSequence(any(), any()) } returns listOf(blockAtHeight(transactions = mockTxList))
    }

    @Bean
    fun txService(): TxService = mockk(relaxed = true)

    @Bean
    fun contractService(): ContractService = mockk(relaxed = true)

    @Bean
    fun nodeInfoService(): NodeInfoService = mockk(relaxed = true)

    @Bean
    fun blockchainEventsService(): BlockchainEventsService = mockk(relaxed = true)

    @Bean
    fun addressService(): AddressService = mockk(relaxed = true)

    @Bean
    fun privacyService(): PrivacyService = mockk(relaxed = true)

    @Bean
    fun utilsService(): UtilsService = mockk(relaxed = true)

    companion object {
        val mockPartition = TxQueuePartition(
            id = "mockPartId",
            priority = 0,
        )

        val createContractTxInAtomicTx = TestDataFactory.executedContractTx(
            tx = TestDataFactory.createContractTx(),
            results = listOf(
                DataEntry(
                    key = DataKey("some_key"),
                    value = DataValue.StringDataValue("fffff"),
                )
            )
        )

        val senderAddress = TestDataFactory.address()

        val atomicBadge = AtomicBadge(senderAddress)

        val sampleCallContractTx = TestDataFactory.callContractTx(
            senderAddress = senderAddress,
            atomicBadge = atomicBadge,
        )

        val sampleCreateContractTx = TestDataFactory.createContractTx(
            senderAddress = senderAddress,
            atomicBadge = atomicBadge,
        )

        val samplePolicyDataHashTx = TestDataFactory.policyDataHashTx(
            senderAddress = senderAddress,
            atomicBadge = atomicBadge,
        )

        val sampleCreatePolicyTx = TestDataFactory.createPolicyTx(
            senderAddress = senderAddress,
            atomicBadge = atomicBadge,
        )

        val sampleUpdatePolicyTx = TestDataFactory.updatePolicyTx(
            senderAddress = senderAddress,
            atomicBadge = atomicBadge,
        )

        val sampleExecutedContractTx = TestDataFactory.executedContractTx()

        val sampleAtomicTx = TestDataFactory.atomicTx(
            senderAddress = senderAddress,
            txs = listOf(
                samplePolicyDataHashTx,
                sampleCreatePolicyTx,
                createContractTxInAtomicTx,
            )
        )

        val fileInfo = TestDataFactory.policyItemFileInfo()

        val privacyInfoResponse = TestDataFactory.policyItemInfoResponse()

        val genesisTx = TestDataFactory.genesisTx()

        val createContractTxWithImageToFilter = TestDataFactory.executedContractTx(
            tx = TestDataFactory.createContractTx(image = ContractImage("image_to_filter")),
            results = listOf(
                DataEntry(
                    key = DataKey("key1_adfdf"),
                    value = DataValue.StringDataValue("fffff"),
                )
            )
        )

        val createContractTxSimple = TestDataFactory.executedContractTx(
            tx = TestDataFactory.createContractTx(),
            results = listOf(
                DataEntry(
                    key = DataKey("key2_adfdf"),
                    value = DataValue.StringDataValue("fffff"),
                )
            )
        )

        val callContractTxSimple = TestDataFactory.executedContractTx(
            tx = TestDataFactory.callContractTx(),
            results = listOf(
                DataEntry(
                    key = DataKey("my_fav_object_string_"),
                    value = DataValue.StringDataValue("my fav object new value"),
                )
            )
        )

        val callContractTxDifferentValueTypes = TestDataFactory.executedContractTx(
            id = TxId("update_tx_for_my_fav_int_and_bool_and_objects".toByteArray()),
            tx = TestDataFactory.callContractTx(),
            results = listOf(
                DataEntry(
                    key = DataKey("my_fav_key_int_"),
                    value = DataValue.IntegerDataValue(123123)
                ),
                DataEntry(
                    key = DataKey("my_fav_object_bool_"),
                    value = DataValue.BooleanDataValue(true)
                ),
                DataEntry(
                    key = DataKey("my_fav_object_map_"),
                    value = DataValue.StringDataValue(
                        """
                                    {
                                        "key_list": [
                                            {
                                                "key_int_1": 1
                                            },
                                            {
                                                "key_int_2": 2
                                            }
                                        ]
                                    }
                        """.trimIndent()
                    )
                ),
                DataEntry(
                    key = DataKey("my_fav_object_object_"),
                    value = DataValue.StringDataValue(
                        """
                                    {
                                        "someString": "string",
                                        "someInt": 10,
                                        "someMap": {
                                            "key": 1
                                        }
                                    }
                        """.trimIndent()
                    )
                ),
            )
        )

        val callContractTxSeveralMatchingKeys = TestDataFactory.executedContractTx(
            id = TxId("update_tx_for_my_fav_several_matching_keys".toByteArray()),
            tx = TestDataFactory.callContractTx(),
            results = listOf(
                DataEntry(
                    key = DataKey("my_fav_multi_key_string_0"),
                    value = DataValue.StringDataValue("123123")
                ),
                DataEntry(
                    key = DataKey("my_fav_multi_key_string_1"),
                    value = DataValue.StringDataValue("123123")
                ),
                DataEntry(
                    key = DataKey("my_fav_multi_key_string_2"),
                    value = DataValue.StringDataValue("123123")
                ),
            )
        )

        val mockTxList = listOf(
            sampleAtomicTx,
            samplePolicyDataHashTx,
            sampleCreatePolicyTx,
            sampleUpdatePolicyTx,
            genesisTx,
            createContractTxWithImageToFilter,
            createContractTxSimple,
            callContractTxSimple,
            callContractTxDifferentValueTypes,
            callContractTxSeveralMatchingKeys
        )
    }
}
