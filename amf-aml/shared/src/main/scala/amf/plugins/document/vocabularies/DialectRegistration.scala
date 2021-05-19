package amf.plugins.document.vocabularies

import amf.client.environment.AMLConfiguration
import amf.client.execution.BaseExecutionEnvironment
import amf.client.parse.DefaultErrorHandler
import amf.client.remod.namespace.AMLDialectNamespaceAliasesPlugin
import amf.client.remod.parsing.AMLDialectInstanceParsingPlugin
import amf.client.remod.rendering.AMLDialectInstanceRenderingPlugin
import amf.core.resolution.pipelines.TransformationPipelineRunner
import amf.internal.environment.Environment
import amf.internal.resource.StringResourceLoader
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.resolution.pipelines.DialectTransformationPipeline
import amf.plugins.document.vocabularies.validation.AMFDialectValidations

import scala.concurrent.{ExecutionContext, Future}

trait DialectRegistration { this: DialectsRegistry =>
  def register(dialect: Dialect): DialectsRegistry = {
    val cloned = dialect.cloneUnit().asInstanceOf[Dialect]
    if (!cloned.resolved) resolveDialect(cloned)
    val profile = new AMFDialectValidations(cloned).profile()
    setEnv {
      env()
        .withPlugins(
            new AMLDialectInstanceParsingPlugin(cloned) ::
              new AMLDialectInstanceRenderingPlugin(cloned) ::
              AMLDialectNamespaceAliasesPlugin(cloned) :: Nil)
        .withValidationProfile(profile)
    }
    invalidateCaches()
    this
  }

  def registerDialect(uri: String, exec: BaseExecutionEnvironment): Future[Dialect] =
    registerDialect(uri, Environment(exec), exec)

  def registerDialect(uri: String,
                      environment: Environment = Environment(),
                      exec: BaseExecutionEnvironment = platform.defaultExecutionEnvironment): Future[Dialect] =
    registerDialect(uri, environment, exec.executionContext)

  def registerDialect(uri: String, environment: Environment, exec: ExecutionContext): Future[Dialect] = {
    implicit val executionContext: ExecutionContext = exec

    instancePlugins
      .find(_.dialect.location().contains(uri))
      .map(p => Future.successful(p.dialect))
      .getOrElse {
        for {
          dialect <- parseDialect(uri, environment)
        } yield {
          invalidateCaches()
          register(dialect)
          dialect
        }
      }
  }

  def registerDialect(url: String, code: String): Future[Dialect] = registerDialect(url, code, Environment())

  def registerDialect(url: String, code: String, exec: BaseExecutionEnvironment): Future[Dialect] =
    registerDialect(url, code, Environment(exec), exec)

  def registerDialect(url: String, code: String, exec: ExecutionContext): Future[Dialect] =
    registerDialect(url, code, Environment(exec), exec)

  def registerDialect(url: String, code: String, env: Environment): Future[Dialect] =
    registerDialect(url, env.add(StringResourceLoader(url, code)))

  def registerDialect(url: String, code: String, env: Environment, exec: BaseExecutionEnvironment): Future[Dialect] =
    registerDialect(url, env.add(StringResourceLoader(url, code)), exec)

  def registerDialect(url: String, code: String, env: Environment, exec: ExecutionContext): Future[Dialect] =
    registerDialect(url, env.add(StringResourceLoader(url, code)), exec)

}

// TODO ARM check this object
private[amf] object DialectRegistration {
  def register(dialect: Dialect, amlConfig: AMLConfiguration): AMLConfiguration = {
    val cloned = dialect.cloneUnit().asInstanceOf[Dialect]
    if (!cloned.resolved) resolveDialect(cloned)
    val profile = new AMFDialectValidations(cloned).profile()
    val newConfig = amlConfig
      .withPlugins(new AMLDialectInstanceParsingPlugin(cloned) :: new AMLDialectInstanceRenderingPlugin(cloned) :: Nil)
      .withValidationProfile(profile)
    newConfig
  }

  private[amf] def resolveDialect(dialect: Dialect) = {
    TransformationPipelineRunner(DefaultErrorHandler()).run(dialect, DialectTransformationPipeline())
    dialect
  }
}
