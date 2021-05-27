package amf.testing.common.utils

import amf.plugins.document.Vocabularies

import amf.plugins.features.validation.AMFValidatorPlugin
import org.mulesoft.common.test.AsyncBeforeAndAfterEach

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.{ExecutionContext, Future}

trait DefaultAMLInitialization extends AsyncBeforeAndAfterEach {
  override protected def beforeEach(): Future[Unit] = DefaultAMLInitialization.init
}

object DefaultAMLInitialization {
  implicit val executionContext: ExecutionContext = Implicits.global
  private var initialized                         = false

  def init: Future[Unit] = {
    if (initialized) Future.successful(Unit)
    else doInit()
  }

  private def doInit(): Future[Unit] = {
    for {
      _ <- amf.core.AMF.init()
      _ <- Future.successful { Vocabularies.register() }
      _ <- Future.successful { amf.core.AMF.registerPlugin(AMFValidatorPlugin) }
      _ <- AMFValidatorPlugin.init()
    } yield {
      initialized = true
    }
  }
}
