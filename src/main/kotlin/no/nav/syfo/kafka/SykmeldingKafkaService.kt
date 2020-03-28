package no.nav.syfo.kafka

import java.time.Duration
import no.nav.syfo.Environment
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.kafka.model.SyfoserviceSykmeldingKafkaMessage
import no.nav.syfo.syfoservice.SyfoserviceMqProducer
import org.apache.kafka.clients.consumer.KafkaConsumer

class SykmeldingKafkaService(
    val syfoserviceMqProducer: SyfoserviceMqProducer,
    val kafkaConsumer: KafkaConsumer<String, SyfoserviceSykmeldingKafkaMessage>,
    val env: Environment,
    val applicationState: ApplicationState
) {

    suspend fun run() {
        kafkaConsumer.subscribe(listOf(env.syfoserviceKafkaTopic))
        while (applicationState.ready) {
            val records = kafkaConsumer.poll(Duration.ofMillis(1000L))
            records.forEach {
                val sykmeldingKafkaMessage = it.value()
                syfoserviceMqProducer.sendTilSyfoservice(
                    sykmeldingKafkaMessage.metadata.sykmeldingId,
                    sykmeldingKafkaMessage.helseopplysninger
                )
            }
        }
    }
}
