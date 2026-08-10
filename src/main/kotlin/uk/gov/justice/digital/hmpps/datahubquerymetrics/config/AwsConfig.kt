package uk.gov.justice.digital.hmpps.datahubquerymetrics.config

import aws.sdk.kotlin.runtime.auth.credentials.AssumeRoleParameters
import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import aws.sdk.kotlin.runtime.auth.credentials.StsAssumeRoleCredentialsProvider
import aws.sdk.kotlin.services.athena.AthenaClient
import aws.sdk.kotlin.services.redshiftdata.RedshiftDataClient
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.cached
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationProperties("aws")
class AwsProperties(
  var region: String = "eu-west-2",
  var accountId: String = "",
  var sts: Sts = Sts(),
) {

  class Sts(
    var tokenRefreshDurationSec: Int = 3600,
    var roleName: String = "dpr-data-api-cross-account-role",
    var roleSessionName: String = "dpr-cross-account-role-session",
  )

  fun getStsRoleArn(): String = "arn:aws:iam::$accountId:role/${sts.roleName}"
}

@Configuration
class AwsConfig {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Bean
  fun stsAssumeRoleCredentialsProvider(properties: AwsProperties): CredentialsProvider {
    log.debug("AWS properties: {}", properties)

    return StsAssumeRoleCredentialsProvider(
      bootstrapCredentialsProvider = DefaultChainCredentialsProvider(),
      region = properties.getRegion(),
      assumeRoleParameters = AssumeRoleParameters(
        roleArn = properties.getStsRoleArn(),
        roleSessionName = properties.sts.roleSessionName,
        duration = properties.sts.tokenRefreshDurationSec.toDuration(DurationUnit.SECONDS),
      ),
    ).cached()
  }

  @Bean
  @ConditionalOnMissingBean(AthenaClient::class)
  @ConditionalOnBean(StsAssumeRoleCredentialsProvider::class)
  fun athenaClient(
    stsAssumeRoleCredentialsProvider: StsAssumeRoleCredentialsProvider,
    properties: AwsProperties,
  ): AthenaClient = AthenaClient {
    region = properties.getRegion()
    credentialsProvider = stsAssumeRoleCredentialsProvider
  }

  @Bean
  @ConditionalOnBean(StsAssumeRoleCredentialsProvider::class)
  fun redshiftDataClient(
    stsAssumeRoleCredentialsProvider: StsAssumeRoleCredentialsProvider,
    properties: AwsProperties,
  ): RedshiftDataClient = RedshiftDataClient {
    region = properties.getRegion()
    credentialsProvider = stsAssumeRoleCredentialsProvider
  }
}
