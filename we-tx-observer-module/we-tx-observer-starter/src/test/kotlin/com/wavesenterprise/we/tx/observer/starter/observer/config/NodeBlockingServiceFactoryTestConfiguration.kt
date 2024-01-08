package com.wavesenterprise.we.tx.observer.starter.observer.config

import com.wavesenterprise.sdk.node.client.blocking.address.AddressService
import com.wavesenterprise.sdk.node.client.blocking.alias.AliasService
import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.sdk.node.client.blocking.contract.ContractService
import com.wavesenterprise.sdk.node.client.blocking.event.BlockchainEventsService
import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.node.client.blocking.node.NodeInfoService
import com.wavesenterprise.sdk.node.client.blocking.pki.PkiService
import com.wavesenterprise.sdk.node.client.blocking.privacy.PrivacyService
import com.wavesenterprise.sdk.node.client.blocking.tx.TxService
import com.wavesenterprise.sdk.node.client.blocking.util.NodeUtilsService
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class NodeBlockingServiceFactoryTestConfiguration {
    @Bean
    fun nodeBlockingServiceFactory(): NodeBlockingServiceFactory = mockk<NodeBlockingServiceFactory>().also {
        every { it.addressService() } returns addressService()
        every { it.aliasService() } returns aliasService()
        every { it.blockchainEventsService() } returns blockchainEventsService()
        every { it.blocksService() } returns blocksService()
        every { it.contractService() } returns contractService()
        every { it.nodeInfoService() } returns nodeInfoService()
        every { it.nodeUtilsService() } returns nodeUtilsService()
        every { it.pkiService() } returns pkiService()
        every { it.privacyService() } returns privacyService()
        every { it.txService() } returns txService()
    }

    @Bean
    fun addressService(): AddressService = mockk()

    @Bean
    fun aliasService(): AliasService = mockk()

    @Bean
    fun blockchainEventsService(): BlockchainEventsService = mockk()

    @Bean
    fun blocksService(): BlocksService = mockk()

    @Bean
    fun contractService(): ContractService = mockk()

    @Bean
    fun nodeInfoService(): NodeInfoService = mockk()

    @Bean
    fun nodeUtilsService(): NodeUtilsService = mockk()

    @Bean
    fun pkiService(): PkiService = mockk()

    @Bean
    fun privacyService(): PrivacyService = mockk()

    @Bean
    fun txService(): TxService = mockk()
}
