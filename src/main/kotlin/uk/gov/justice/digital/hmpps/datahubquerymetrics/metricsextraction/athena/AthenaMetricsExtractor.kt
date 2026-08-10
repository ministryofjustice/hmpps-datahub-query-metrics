package uk.gov.justice.digital.hmpps.datahubquerymetrics.metricsextraction.athena

import aws.sdk.kotlin.services.athena.AthenaClient
import aws.sdk.kotlin.services.athena.batchGetQueryExecution
import aws.sdk.kotlin.services.athena.listQueryExecutions
import aws.sdk.kotlin.services.athena.model.AthenaException
import aws.sdk.kotlin.services.athena.model.QueryExecution
import aws.sdk.kotlin.services.athena.model.QueryExecutionState
import aws.sdk.kotlin.services.sts.StsClient
import aws.sdk.kotlin.services.sts.getCallerIdentity
import aws.sdk.kotlin.services.sts.model.GetCallerIdentityRequest
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.collections.AttributeKey
import aws.smithy.kotlin.runtime.time.toJvmInstant
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.datahubquerymetrics.metricsextraction.MetricsExtractor
import uk.gov.justice.digital.hmpps.datahubquerymetrics.metricsextraction.QueryEngine
import uk.gov.justice.digital.hmpps.datahubquerymetrics.metricsextraction.QueryExecutionStatus
import uk.gov.justice.digital.hmpps.datahubquerymetrics.metricsextraction.SingleQueryMetricsInfo
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Service
class AthenaMetricsExtractor(
  @Value($$"${dpr.athena.workgroup}") private val athenaWorkgroup: String,
  private val athenaClient: AthenaClient,
) : MetricsExtractor {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @OptIn(InternalApi::class)
  override suspend fun extractQueryMetrics(): Collection<SingleQueryMetricsInfo> {
    println(
      """
      ${athenaClient.config.region}
      ${athenaClient.config.toString()}
      """.trimIndent(),
    )
    StsClient.fromEnvironment().use { sts ->
      val identity = sts.getCallerIdentity(GetCallerIdentityRequest {})
      println("acc: ${identity.account} || arn: ${identity.arn} || userid: ${identity.userId}")
    }
    val nowUtc = Instant.now()
    val startTime = nowUtc.minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS)
    val endTime = nowUtc.minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS)
    val validExecutions = mutableListOf<QueryExecution>()
    var token: String? = null
    while (true) {
      val executionsList = runCatching {
        athenaClient.listQueryExecutions {
          workGroup = athenaWorkgroup
          maxResults = 50
          nextToken = token
        }
      }.onFailure {
        when (it) {
          is AthenaException -> {
            log.error(
              """
            msg: ${it.message}\n
            
            trace: ${it.stackTraceToString()}\n
            
            cause: ${it.cause}\n
            
            sdkmetadata: ${it.sdkErrorMetadata.errorCode} || ${it.sdkErrorMetadata.errorType.name} || ${it.sdkErrorMetadata.errorMessage} || ${it.sdkErrorMetadata.protocolResponse.summary} || ${it.sdkErrorMetadata.clientContext.joinToString("|-|")} || ${it.sdkErrorMetadata.attributes.toString()} \n
              """.trimIndent(), it)
            throw(it.cause as AthenaException)
          }
          else -> {
            log.error("Other error: ${it.message} || \n${it.stackTraceToString()}", it)
            throw(it)
          }
        }
      }.getOrNull()!!
      token = executionsList.nextToken
      if (executionsList.queryExecutionIds.isNullOrEmpty()) {
        break
      }
      val batchGetQueryExecutionResult = athenaClient.batchGetQueryExecution { queryExecutionIds = executionsList.queryExecutionIds!! }
      if (batchGetQueryExecutionResult.queryExecutions.isNullOrEmpty()) {
        throw IllegalStateException("There are no batch executions found for the given list somehow")
      }
      val executions = batchGetQueryExecutionResult.queryExecutions!!.sortedBy { it.status!!.completionDateTime }
      if (executions.first().status!!.state!! == QueryExecutionState.Running || executions.first().status!!.completionDateTime!!.toJvmInstant() <= startTime) {
        break
      }
      val filteredExecutions = executions.filter {
        it.status!!.completionDateTime!!.toJvmInstant() <= endTime && it.status!!.completionDateTime!!.toJvmInstant() > startTime
      }
      validExecutions.addAll(filteredExecutions)
      if (token == null) {
        break
      }
    }

    return validExecutions.map {
      val queryInfo = extractQueryInfo(it.query!!)

      SingleQueryMetricsInfo(
        it.queryExecutionId!!,
        it.query!!,
        ZonedDateTime.ofInstant(it.status!!.submissionDateTime!!.toJvmInstant(), ZoneId.of("UTC")),
        ZonedDateTime.ofInstant(it.status!!.completionDateTime!!.toJvmInstant(), ZoneId.of("UTC")),
        QueryExecutionStatus.fromAthenaState(it.status!!.state!!),
        it.statistics!!.totalExecutionTimeInMillis!! * 1000L,
        QueryEngine.ATHENA,
        queryInfo.productId,
        queryInfo.productName,
        queryInfo.datasourceName,
        queryInfo.datasourceCatalog,
        queryInfo.datasourceName,
        queryInfo.reportOrDashboardId,
        queryInfo.hasProbationDatasources,
      )
    }
  }
}
