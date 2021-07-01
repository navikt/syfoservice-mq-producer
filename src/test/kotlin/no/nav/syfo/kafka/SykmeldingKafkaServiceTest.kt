package no.nav.syfo.kafka

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.Environment
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.kafka.model.KafkaMessageMetadata
import no.nav.syfo.kafka.model.SyfoserviceSykmeldingKafkaMessage
import no.nav.syfo.kafka.util.JacksonKafkaDeserializer
import no.nav.syfo.syfoservice.SyfoserviceMqProducer
import no.nav.syfo.syfoservice.Tilleggsdata
import no.nav.syfo.syfoservice.getHelseOpplysningerArbeidsuforhet
import no.nav.syfo.syfoservice.objectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.LocalDateTime
import java.util.Properties

class SykmeldingKafkaServiceTest : Spek({
    val kafka = KafkaContainer(DockerImageName.parse(KAFKA_IMAGE_NAME).withTag(KAFKA_IMAGE_VERSION))
    kafka.start()
    val env = mockkClass(Environment::class)
    every { env.applicationName } returns "application"
    every { env.syfoserviceKafkaTopic } returns "topic"

    val kafkaConfig = Properties()
    kafkaConfig.let {
        it["bootstrap.servers"] = kafka.bootstrapServers
        it[ConsumerConfig.GROUP_ID_CONFIG] = "groupId"
        it[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        it[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JacksonKafkaDeserializer::class.java
        it[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        it[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
    }
    val kafkaProducer = KafkaProducer<String, String>(kafkaConfig)
    val kafkaConsumer = KafkaConsumer<String, SyfoserviceSykmeldingKafkaMessage>(kafkaConfig)
    val applicationState = ApplicationState(alive = true, ready = true)

    val syfoserviceMqProducer = mockk<SyfoserviceMqProducer>(relaxed = true)

    val sykmeldingKafkaService = SykmeldingKafkaService(syfoserviceMqProducer, kafkaConsumer, env, applicationState)

    every { syfoserviceMqProducer.sendTilSyfoservice(any(), any()) } answers {
        applicationState.ready = false
        applicationState.alive = false
    }

    beforeEachTest {
        applicationState.ready = true
        applicationState.alive = true
    }

    describe("Test SykmeldingKafkaService") {
        it("should read from kafka") {
            val message = SyfoserviceSykmeldingKafkaMessage(
                KafkaMessageMetadata(
                    sykmeldingId = "123", source = "egenmeldt-sykmelding-backend"
                ),
                getHelseOpplysningerArbeidsuforhet(),
                Tilleggsdata("1", "123", "1", LocalDateTime.now())
            )
            kafkaProducer.send(ProducerRecord("topic", objectMapper.writeValueAsString(message)))

            runBlocking {
                sykmeldingKafkaService.run()
            }

            verify(exactly = 1) { syfoserviceMqProducer.sendTilSyfoservice(any(), any()) }
        }
    }
})
