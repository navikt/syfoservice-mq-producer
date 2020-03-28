package no.nav.syfo.syfoservice

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import javax.jms.MessageProducer
import javax.jms.Session
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class SyfoserviceMqProducerTest : Spek({
    val session = mockk<Session>()
    val messageProducer = mockk<MessageProducer>(relaxed = true)
    val syfoserviceMqProducer = SyfoserviceMqProducer(session, messageProducer)
    every { session.createTextMessage() } returns mockk(relaxed = true)
    describe("SyfoserviceMqProducerTest") {
        it("Should write to mq") {
            syfoserviceMqProducer.sendTilSyfoservice("123", getHelseOpplysningerArbeidsuforhet())
            verify(exactly = 1) { messageProducer.send(any()) }
        }
    }
})
