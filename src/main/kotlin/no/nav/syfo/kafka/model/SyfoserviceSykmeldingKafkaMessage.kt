package no.nav.syfo.kafka.model

import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet

data class SyfoserviceSykmeldingKafkaMessage(
    val metadata: KafkaMessageMetadata,
    val helseopplysninger: HelseOpplysningerArbeidsuforhet
)
