package no.nav.syfo.syfoservice

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.helse.sm2013.Address
import no.nav.helse.sm2013.ArsakType
import no.nav.helse.sm2013.CS
import no.nav.helse.sm2013.CV
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.helse.sm2013.Ident
import no.nav.helse.sm2013.NavnType
import no.nav.helse.sm2013.TeleCom
import java.time.LocalDate
import java.time.LocalDateTime

fun getHelseOpplysningerArbeidsuforhet(): HelseOpplysningerArbeidsuforhet {
    return HelseOpplysningerArbeidsuforhet().apply {
        arbeidsgiver = HelseOpplysningerArbeidsuforhet.Arbeidsgiver().apply {
            harArbeidsgiver = CS().apply {
                dn = "En arbeidsgiver"
                v = "1"
            }
            navnArbeidsgiver = "SAS as"
            yrkesbetegnelse = "Pilot"
            stillingsprosent = 100
        }
        kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
            kontaktDato = LocalDate.now()
            behandletDato = LocalDateTime.now()
        }
        behandler = HelseOpplysningerArbeidsuforhet.Behandler().apply {
            navn = NavnType().apply {
                fornavn = "Per"
                etternavn = "Hansne"
            }
            id.add(
                Ident().apply {
                    id = "12343567"
                    typeId = CV().apply {
                        dn = "Fødselsnummer"
                        s = "2.16.578.1.12.4.1.1.8116"
                        v = "FNR"
                    }
                }
            )
            adresse = Address().apply {
            }
            kontaktInfo.add(
                TeleCom().apply {
                }
            )
        }
        aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
            periode.add(
                HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                    periodeFOMDato = LocalDate.now()
                    periodeTOMDato = LocalDate.now().plusDays(4)
                    aktivitetIkkeMulig = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AktivitetIkkeMulig().apply {
                        medisinskeArsaker = ArsakType().apply {
                            arsakskode.add(
                                CS().apply {
                                    v = "1"
                                    dn = "Helsetilstanden hindrer pasienten i å være i aktivitet"
                                }
                            )
                            beskriv = "Kan ikkje jobbe"
                        }
                    }
                }
            )
        }
        pasient = HelseOpplysningerArbeidsuforhet.Pasient().apply {
            fodselsnummer = Ident().apply {
                id = "12343567"
                typeId = CV().apply {
                    dn = "Fødselsnummer"
                    s = "2.16.578.1.12.4.1.1.8116"
                    v = "FNR"
                }
            }
        }
        syketilfelleStartDato = LocalDate.now()
        medisinskVurdering = HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
            hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode = CV().apply {
                    dn = "Problem med jus/politi"
                    s = "2.16.578.1.12.4.1.1.7110"
                    v = "Z09"
                }
            }
        }
        avsenderSystem = HelseOpplysningerArbeidsuforhet.AvsenderSystem().apply {
            systemNavn = "EGENMELDT"
            systemVersjon = "1.0.2"
        }
    }
}

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}
