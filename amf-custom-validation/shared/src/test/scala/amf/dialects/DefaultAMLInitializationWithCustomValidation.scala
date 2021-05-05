package amf.dialects

import amf.client.environment.AMLConfiguration
import amf.core.registries.AMFPluginsRegistry
import amf.plugins.features.validation.custom.AMFValidatorPlugin
import org.mulesoft.common.test.AsyncBeforeAndAfterEach

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.{ExecutionContext, Future}

trait DefaultAMLInitializationWithCustomValidation extends AsyncBeforeAndAfterEach {
  override protected def beforeEach(): Future[Unit] = DefaultAMLInitializationWithCustomValidation.init
}

object DefaultAMLInitializationWithCustomValidation {
  implicit val executionContext: ExecutionContext = Implicits.global
  private var initialized                         = false

  def init: Future[Unit] = {
    if (initialized) Future.successful(Unit)
    else doInit()
  }

  private def doInit(): Future[Unit] = {
    for {
      _ <- amf.core.AMF.init()
      _ <- Future.successful { AMFPluginsRegistry.staticConfiguration = AMLConfiguration.predefined() }
      _ <- Future.successful { amf.core.AMF.registerPlugin(AMFValidatorPlugin) }
      _ <- AMFValidatorPlugin.init()
    } yield {
      initialized = true
    }
  }
}
