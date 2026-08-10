package uk.gov.justice.digital.hmpps.datahubquerymetrics.prometheus

import io.prometheus.metrics.exporter.httpserver.HTTPServer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PrometheusServer {

  @Bean(destroyMethod = "close")
  fun prometheusEndpoint(): HTTPServer = HTTPServer.builder().port(9400).buildAndStart()
}
