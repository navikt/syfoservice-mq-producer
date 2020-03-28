package no.nav.syfo

import io.prometheus.client.hotspot.DefaultExports
import javax.jms.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.kafka.SykmeldingKafkaService
import no.nav.syfo.kafka.envOverrides
import no.nav.syfo.kafka.loadBaseConfig
import no.nav.syfo.kafka.model.SyfoserviceSykmeldingKafkaMessage
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.kafka.util.JacksonKafkaDeserializer
import no.nav.syfo.mq.connectionFactory
import no.nav.syfo.mq.producerForQueue
import no.nav.syfo.syfoservice.SyfoserviceMqProducer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfoservice-mq-producer")

fun main() {
    val env = Environment()
    val vaultSecrets = VaultSecrets()
    DefaultExports.initialize()
    val applicationState = ApplicationState()

    val connection = connectionFactory(env).createConnection(vaultSecrets.mqUsername, vaultSecrets.mqPassword)

    connection.start()
    val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    val syfoserviceProducer = session.producerForQueue(env.syfoserviceQueueName)
    val syfoserviceMqProducer = SyfoserviceMqProducer(session, syfoserviceProducer)

    val kafkaBaseConfig = loadBaseConfig(env, vaultSecrets).envOverrides()
    val consumerProperties = kafkaBaseConfig.toConsumerConfig(
        "${env.applicationName}-consumer",
        JacksonKafkaDeserializer::class
    )
    consumerProperties.let { it[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1" }

    val kafkaConsumer = KafkaConsumer<String, SyfoserviceSykmeldingKafkaMessage>(consumerProperties)

    val sykmelidngKafkaService = SykmeldingKafkaService(syfoserviceMqProducer, kafkaConsumer, env, applicationState)

    val applicationEngine = createApplicationEngine(
        env,
        applicationState
    )
    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()
    applicationState.ready = true

    createListener(applicationState) {
        sykmelidngKafkaService.run()
    }
}

fun createListener(applicationState: ApplicationState, action: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch {
        try {
            action()
        } catch (ex: Exception) {
            log.error("Noe gikk galt", ex.cause)
        } finally {
            applicationState.alive = false
        }
    }
