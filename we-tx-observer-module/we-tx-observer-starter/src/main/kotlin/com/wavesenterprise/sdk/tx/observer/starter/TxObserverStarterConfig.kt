package com.wavesenterprise.sdk.tx.observer.starter

import com.fasterxml.jackson.databind.ObjectMapper
import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.spring.autoconfigure.node.NodeBlockingServiceFactoryAutoConfiguration
import com.wavesenterprise.sdk.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.sdk.tx.observer.core.spring.method.callback.PrivateContentResolverImpl
import com.wavesenterprise.sdk.tx.observer.starter.annotation.EventListenerAnnotationProcessor
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.expression.BeanFactoryResolver

@Configuration
@AutoConfigureAfter(
    DataSourceAutoConfiguration::class,
    NodeBlockingServiceFactoryAutoConfiguration::class,
)
@Import(JpaExecutorsConfig::class)
class TxObserverStarterConfig {

    @Autowired
    lateinit var beanFactory: ConfigurableBeanFactory

    @Bean
    fun beanFactoryResolver() = BeanFactoryResolver(beanFactory)

    @Bean
    fun listenersAnnotationProcessor(
        privateContentResolverProvider: ObjectProvider<PrivateContentResolver>,
        objectMapper: ObjectMapper, // todo bean ref from props
    ) = EventListenerAnnotationProcessor(
        beanFactory = beanFactory,
        beanFactoryResolver = beanFactoryResolver(),
        privateContentResolverProvider = privateContentResolverProvider,
        objectMapper = objectMapper,
    )

    @Bean
    @ConditionalOnBean(NodeBlockingServiceFactory::class)
    @ConditionalOnMissingBean
    fun nodeApiPrivateContentResolver(
        nodeBlockingServiceFactory: NodeBlockingServiceFactory,
        objectMapper: ObjectMapper,
    ): PrivateContentResolver =
        PrivateContentResolverImpl(
            nodeBlockingServiceFactory = nodeBlockingServiceFactory,
            objectMapper = objectMapper,
        )
}
