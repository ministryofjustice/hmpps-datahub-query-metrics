package uk.gov.justice.digital.hmpps.datahubquerymetrics.metricsextraction.redshift

import aws.sdk.kotlin.services.redshiftdata.RedshiftDataClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedshiftClientConfig {

  @Bean
  fun redshiftDataClient() = RedshiftDataClient.builder().build()
}
