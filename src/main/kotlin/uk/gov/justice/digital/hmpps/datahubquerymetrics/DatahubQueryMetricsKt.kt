package uk.gov.justice.digital.hmpps.datahubquerymetrics

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class DatahubQueryMetricsKt

fun main(args: Array<String>) {
  runApplication<DatahubQueryMetricsKt>(*args)
}
