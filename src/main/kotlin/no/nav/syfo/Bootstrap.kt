package no.nav.syfo

import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.kafka.SykmeldingKafkaService
import no.nav.syfo.kafka.aiven.KafkaUtils
import no.nav.syfo.kafka.model.SyfoserviceSykmeldingKafkaMessage
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.kafka.util.JacksonKafkaDeserializer
import no.nav.syfo.mq.connectionFactory
import no.nav.syfo.mq.producerForQueue
import no.nav.syfo.syfoservice.SyfoserviceMqProducer
import no.nav.syfo.util.Unbounded
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.jms.Session

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfoservice-mq-producer")

@DelicateCoroutinesApi
fun main() {
    val env = Environment()
    val vaultSecrets = VaultSecrets()
    DefaultExports.initialize()
    val applicationState = ApplicationState()

    val syfoserviceMqProducer = createSyfoservieProducer(env, vaultSecrets)

    val kafkaConsumerAivenConfig = KafkaUtils.getAivenKafkaConfig().toConsumerConfig(
        "${env.applicationName}-consumer",
        JacksonKafkaDeserializer::class
    ).also {
        it["auto.offset.reset"] = "none"
        it[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1"
    }
    val kafkaConsumerAiven = KafkaConsumer<String, SyfoserviceSykmeldingKafkaMessage>(kafkaConsumerAivenConfig)

    val sykmeldingKafkaAivenService = SykmeldingKafkaService(syfoserviceMqProducer, kafkaConsumerAiven, env.syfoserviceKafkaTopicAiven, applicationState, "aiven")

    val applicationEngine = createApplicationEngine(
        env,
        applicationState
    )
    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()

    applicationState.ready = true

    createListener(applicationState) {
        sykmeldingKafkaAivenService.run()
    }
}

private fun createSyfoservieProducer(
    env: Environment,
    vaultSecrets: VaultSecrets
): SyfoserviceMqProducer {
    val connection =
        connectionFactory(env).createConnection(vaultSecrets.serviceuserUsername, vaultSecrets.serviceuserPassword)

    connection.start()
    val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    val syfoserviceProducer = session.producerForQueue(env.syfoserviceQueueName)
    val syfoserviceMqProducer = SyfoserviceMqProducer(session, syfoserviceProducer)
    return syfoserviceMqProducer
}

@DelicateCoroutinesApi
fun createListener(applicationState: ApplicationState, action: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch(Dispatchers.Unbounded) {
        try {
            action()
        } catch (ex: Exception) {
            log.error("Noe gikk galt", ex.cause)
        } finally {
            applicationState.alive = false
            applicationState.ready = false
        }
    }
