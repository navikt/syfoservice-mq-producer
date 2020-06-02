package no.nav.syfo.kafka

import java.time.Duration
import kotlinx.coroutines.delay
import no.nav.syfo.Environment
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.metrics.SYKMELDING_MQ_PRODUCER_COUNTER
import no.nav.syfo.kafka.model.SyfoserviceSykmeldingKafkaMessage
import no.nav.syfo.log
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
                log.info("lest sykmelding fra topic og publiserer til syfoservice-mq, sykmeldingId {}", sykmeldingKafkaMessage.metadata.sykmeldingId)
                syfoserviceMqProducer.sendTilSyfoservice(
                    sykmeldingKafkaMessage.helseopplysninger,
                    sykmeldingKafkaMessage.tilleggsdata
                )
                SYKMELDING_MQ_PRODUCER_COUNTER.labels(sykmeldingKafkaMessage.metadata.source).inc()
            }
            delay(1)
        }
    }
}
