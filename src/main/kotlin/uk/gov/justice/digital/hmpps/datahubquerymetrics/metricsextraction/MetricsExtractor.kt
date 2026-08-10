package uk.gov.justice.digital.hmpps.datahubquerymetrics.metricsextraction

import org.slf4j.LoggerFactory

data class QueryInfo(
  val productId: String,
  val productName: String,
  val datasourceName: String,
  val datasourceCatalog: String?,
  val databaseName: String?,
  val reportOrDashboardId: String,
  val hasProbationDatasources: Boolean,
)

interface MetricsExtractor {
  suspend fun extractQueryMetrics(): Collection<SingleQueryMetricsInfo>

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun takeStringOrNull(str: String): String? = if (str.isNotBlank() && str != "null") str else null

  fun extractQueryInfo(query: String?): QueryInfo {
    if (!query!!.contains("QUERY_INFO")) {
      return QueryInfo("", "", "", "", "", "", false)
    }
    val queryInfo = query.substringAfter("QUERY_INFO|||").substringBefore("|||END").split("|||")

    return QueryInfo(
      queryInfo[0],
      queryInfo[1],
      queryInfo[2],
      takeStringOrNull(queryInfo[3]),
      takeStringOrNull(queryInfo[4]),
      queryInfo[5],
      queryInfo[6].toBoolean(),
    )
  }
}
