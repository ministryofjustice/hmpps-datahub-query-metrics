package uk.gov.justice.digital.hmpps.datahubquerymetrics.config

import aws.sdk.kotlin.runtime.auth.credentials.AssumeRoleParameters
import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import aws.sdk.kotlin.runtime.auth.credentials.StsAssumeRoleCredentialsProvider
import aws.sdk.kotlin.services.athena.AthenaClient
import aws.sdk.kotlin.services.redshiftdata.RedshiftDataClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Configuration
class AwsConfig(
  @Value($$"${aws.accountid}") private val accountId: String,
) {
  private val awsRegion = "eu-west-2"
  private val tokenRefreshDurationSec: Int = 3600
  private val roleName: String = "dpr-data-api-cross-account-role"
  private val roleSessionName: String = "dpr-cross-account-role-session"
  private val stsRoleArn: String = "arn:aws:iam::$accountId:role/$roleName"

  @Bean
  fun stsAssumeRoleCredentialsProvider(): StsAssumeRoleCredentialsProvider {
    return StsAssumeRoleCredentialsProvider(
      bootstrapCredentialsProvider = DefaultChainCredentialsProvider(),
      region = awsRegion,
      assumeRoleParameters = AssumeRoleParameters(
        roleArn = stsRoleArn,
        roleSessionName = roleSessionName,
        duration = tokenRefreshDurationSec.toDuration(DurationUnit.SECONDS),
      ),
    )
  }

  @Bean
  @ConditionalOnBean(StsAssumeRoleCredentialsProvider::class)
  fun athenaClient(
    stsAssumeRoleCredentialsProvider: StsAssumeRoleCredentialsProvider,
  ): AthenaClient = AthenaClient {
    region = awsRegion
    credentialsProvider = stsAssumeRoleCredentialsProvider
  }

  @Bean
  @ConditionalOnBean(StsAssumeRoleCredentialsProvider::class)
  fun redshiftDataClient(
    stsAssumeRoleCredentialsProvider: StsAssumeRoleCredentialsProvider,
  ): RedshiftDataClient = RedshiftDataClient {
    region = awsRegion
    credentialsProvider = stsAssumeRoleCredentialsProvider
  }
}
