package amf.dialects

import amf.plugins.document.Vocabularies
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.domain.VocabulariesRegister
import amf.plugins.features.validation.AMFValidatorPlugin
import org.scalactic.source.Position
import org.scalatest.compatible.Assertion
import org.scalatest.{AsyncFunSuite, Tag}

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.{ExecutionContext, Future}

trait DefaultAmfInitialization extends AsyncBeforeAndAfterAll {
  override protected def beforeAll(): Future[Unit] = DefaultAmfInitialization.init
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

trait AsyncBeforeAndAfterAll extends AsyncFunSuite {
  override protected def test(testName: String, testTags: Tag*)(testFun: => Future[Assertion])(
      implicit pos: Position): Unit = {
    lazy val composedFn = for {
      _         <- beforeAll()
      assertion <- testFun
      _         <- afterAll()
    } yield {
      assertion
    }
    super.test(testName, testTags: _*)(composedFn)
  }

  protected def beforeAll(): Future[Unit] = Future.successful(Unit)
  protected def afterAll(): Future[Unit]  = Future.successful(Unit)
}
