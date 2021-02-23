package amf.dialects

import amf.core.registries.AMFPluginsRegistry
import amf.plugins.document.graph.AMFGraphParsePlugin
import amf.plugins.document.vocabularies.AMLParsePlugin
import amf.plugins.features.validation.custom.AMFValidatorPlugin
import org.mulesoft.common.test.AsyncBeforeAndAfterEach
import org.scalactic.source.Position
import org.scalatest.{AsyncFunSuite, Tag}
import org.scalatest.compatible.Assertion

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.{ExecutionContext, Future}

trait DefaultAmfInitializationWithCustomValidation extends AsyncBeforeAndAfterEach {
  override protected def beforeEach(): Future[Unit] = DefaultAmfInitializationWithCustomValidation.init
}

object DefaultAmfInitializationWithCustomValidation {
  implicit val executionContext: ExecutionContext = Implicits.global
  private var initialized                         = false

  def init: Future[Unit] = {
    if (initialized) Future.successful(Unit)
    else doInit()
  }

  private def doInit(): Future[Unit] = {
    for {
      _ <- amf.core.AMF.init()
      _ <- Future.successful {
        amf.core.AMF.registerPlugin(AMFValidatorPlugin)
        AMFPluginsRegistry.registerNewInterfacePlugin(AMLParsePlugin)
        AMFPluginsRegistry.registerNewInterfacePlugin(AMFGraphParsePlugin)
      }
      _ <- AMFValidatorPlugin.init()
    } yield {
      initialized = true
    }
  }
}
