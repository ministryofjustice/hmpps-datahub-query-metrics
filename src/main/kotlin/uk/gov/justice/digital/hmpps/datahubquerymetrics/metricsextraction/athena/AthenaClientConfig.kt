package uk.gov.justice.digital.hmpps.datahubquerymetrics.metricsextraction.athena

import aws.sdk.kotlin.services.athena.AthenaClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AthenaClientConfig {

  @Bean
  fun athenaClient() = AthenaClient.builder().build()
}
