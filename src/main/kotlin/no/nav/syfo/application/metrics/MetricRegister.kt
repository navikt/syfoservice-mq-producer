package no.nav.syfo.application.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Histogram

const val METRICS_NS = ""

val HTTP_HISTOGRAM: Histogram = Histogram.Builder()
    .labelNames("path")
    .name("requests_duration_seconds")
    .help("http requests durations for incoming requests in seconds")
    .register()

val SYKMELDING_MQ_PRODUCER_COUNTER: Counter = Counter.build()
    .labelNames("source")
    .help("sykmeldinger received on kafka topic and written to syfoservice mq")
    .name("sykmelding_mq_producer_counter")
    .register()
