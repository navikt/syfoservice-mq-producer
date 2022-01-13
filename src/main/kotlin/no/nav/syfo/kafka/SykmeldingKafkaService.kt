package no.nav.syfo.kafka

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.metrics.SYKMELDING_MQ_PRODUCER_COUNTER
import no.nav.syfo.kafka.model.SyfoserviceSykmeldingKafkaMessage
import no.nav.syfo.log
import no.nav.syfo.syfoservice.SyfoserviceMqProducer
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

class SykmeldingKafkaService(
    val syfoserviceMqProducer: SyfoserviceMqProducer,
    val kafkaConsumer: KafkaConsumer<String, SyfoserviceSykmeldingKafkaMessage>,
    val topic: String,
    val applicationState: ApplicationState,
    val source: String = "on-prem"
) {

    fun run() {
        kafkaConsumer.subscribe(listOf(topic))
        log.info("Starting $source consumer")
        while (applicationState.ready) {
            val records = kafkaConsumer.poll(Duration.ofMillis(1000L))
            records.forEach {
                val sykmeldingKafkaMessage = it.value()
                log.info("$source: lest sykmelding fra topic og publiserer til syfoservice-mq, sykmeldingId {}", sykmeldingKafkaMessage.metadata.sykmeldingId)
                syfoserviceMqProducer.sendTilSyfoservice(
                    sykmeldingKafkaMessage.helseopplysninger,
                    sykmeldingKafkaMessage.tilleggsdata
                )
                SYKMELDING_MQ_PRODUCER_COUNTER.labels(sykmeldingKafkaMessage.metadata.source).inc()
            }
        }
    }
}
