package amf.dialects

import amf.plugins.document.Vocabularies
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.domain.VocabulariesRegister
import amf.plugins.features.validation.AMFValidatorPlugin
import org.mulesoft.common.test.AsyncBeforeAndAfterEach
import org.scalactic.source.Position
import org.scalatest.compatible.Assertion
import org.scalatest.{AsyncFunSuite, Tag}

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.{ExecutionContext, Future}

trait DefaultAmfInitialization extends AsyncBeforeAndAfterEach {
  override protected def beforeEach(): Future[Unit] = DefaultAmfInitialization.init
}

object DefaultAmfInitialization {
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
      _ <- AMLPlugin.init()
      _ <- Future.successful { amf.core.AMF.registerPlugin(AMFValidatorPlugin) }
      _ <- AMFValidatorPlugin.init()
    } yield {
      initialized = true
    }
  }
}
