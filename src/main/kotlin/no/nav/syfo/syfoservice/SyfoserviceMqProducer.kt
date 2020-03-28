package no.nav.syfo.syfoservice

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Base64
import javax.jms.MessageProducer
import javax.jms.Session
import javax.jms.TextMessage
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet

// Not threadsafe
class SyfoserviceMqProducer(
    private val session: Session,
    private val messageProducer: MessageProducer
) {
    private val sykmeldingMarshaller: Marshaller = JAXBContext.newInstance(HelseOpplysningerArbeidsuforhet::class.java).createMarshaller()
        .apply { setProperty(Marshaller.JAXB_ENCODING, "UTF-8") }
    private val xmlObjectWriter: XmlMapper = XmlMapper().apply {
        registerModule(JavaTimeModule())
        registerKotlinModule()
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

    fun sendTilSyfoservice(
        sykmeldingId: String,
        healthInformation: HelseOpplysningerArbeidsuforhet
    ) {
        messageProducer.send(session.createTextMessage().apply(createMessage(healthInformation, sykmeldingId)))
    }

    private fun createMessage(
        healthInformation: HelseOpplysningerArbeidsuforhet,
        sykmeldingId: String
    ): TextMessage.() -> Unit {
        return {
            val syketilfelleStartDato = extractSyketilfelleStartDato(healthInformation)
            val sykmelding = convertSykemeldingToBase64(healthInformation, sykmeldingMarshaller)
            val syfo = Syfo(
                tilleggsdata = Tilleggsdata(
                    ediLoggId = sykmeldingId,
                    sykmeldingId = sykmeldingId,
                    msgId = sykmeldingId,
                    syketilfelleStartDato = syketilfelleStartDato
                ),
                sykmelding = Base64.getEncoder().encodeToString(sykmelding)
            )
            text = xmlObjectWriter.writeValueAsString(syfo)
        }
    }

    private fun convertSykemeldingToBase64(
        helseOpplysningerArbeidsuforhet: HelseOpplysningerArbeidsuforhet,
        sykmeldingMarshaller: Marshaller
    ): ByteArray =
        ByteArrayOutputStream().use {
            sykmeldingMarshaller.marshal(helseOpplysningerArbeidsuforhet, it)
            it
        }.toByteArray()

    private fun extractSyketilfelleStartDato(helseOpplysningerArbeidsuforhet: HelseOpplysningerArbeidsuforhet): LocalDateTime =
        LocalDateTime.of(helseOpplysningerArbeidsuforhet.syketilfelleStartDato, LocalTime.NOON)
}
