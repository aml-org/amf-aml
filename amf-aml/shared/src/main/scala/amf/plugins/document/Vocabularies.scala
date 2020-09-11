package amf.plugins.document

import amf.client.convert.VocabulariesClientConverter._
import amf.client.environment.Environment
import amf.client.model.document.Dialect
import amf.core.unsafe.PlatformSecrets
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.domain.VocabulariesRegister

import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
object Vocabularies extends PlatformSecrets {

  def register(): Unit = {
    VocabulariesRegister.register(platform)
    amf.Core.registerPlugin(AMLPlugin)
  }

  def registerDialect(url: String): ClientFuture[Dialect] = {
    implicit val executionContext: ExecutionContext = platform.defaultExecutionEnvironment.executionContext
    AMLPlugin().registry.registerDialect(url).asClient
  }

  def registerDialect(url: String, env: Environment): ClientFuture[Dialect] = {
    implicit val executionContext: ExecutionContext = env.executionEnvironment.executionContext
    AMLPlugin().registry.registerDialect(url, env._internal, env.executionEnvironment).asClient
  }

  def registerDialect(url: String, dialectText: String): ClientFuture[Dialect] = {
    implicit val executionContext: ExecutionContext = platform.defaultExecutionEnvironment.executionContext
    AMLPlugin().registry.registerDialect(url, dialectText).asClient
  }

  def registerDialect(url: String, dialectText: String, env: Environment): ClientFuture[Dialect] = {
    implicit val executionContext: ExecutionContext = env.executionEnvironment.executionContext
    AMLPlugin().registry.registerDialect(url, dialectText, env._internal, env.executionEnvironment).asClient
  }
}
