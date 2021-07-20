package org.qask.backend.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.qask.backend.models.viewModels.QuestionInferOutput
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer


@EnableKafka
@Configuration
class KafkaConsumerConfig {

    @Value(value = "\${kafka.bootstrapAddress}")
    lateinit var bootstrapAddress: String

    fun consumerFactory(groupId: String): ConsumerFactory<String, QuestionInferOutput> {
        val props: MutableMap<String, Any> = HashMap()
        val deserializer = JsonDeserializer(
            QuestionInferOutput::class.java
        )
        deserializer.setRemoveTypeHeaders(false)
        deserializer.addTrustedPackages("*")
        deserializer.setUseTypeMapperForKey(true)

        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapAddress
        props[ConsumerConfig.GROUP_ID_CONFIG] = groupId
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
        return DefaultKafkaConsumerFactory(props, StringDeserializer(), deserializer)
    }

    fun kafkaListenerContainerFactory(groupId: String): ConcurrentKafkaListenerContainerFactory<String, QuestionInferOutput> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, QuestionInferOutput>()
        factory.consumerFactory = consumerFactory(groupId)
        return factory
    }

    @Bean
    fun insincereQuestionListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, QuestionInferOutput> {
        return kafkaListenerContainerFactory("insincere-question-spring-consumer")
    }

}
