package uk.gov.justice.digital.hmpps.datahubquerymetrics

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DatahubQueryMetrics

fun main(args: Array<String>) {
  runApplication<DatahubQueryMetrics>(*args)
}
