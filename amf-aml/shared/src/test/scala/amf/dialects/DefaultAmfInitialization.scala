package amf.dialects

import amf.plugins.features.validation.AMFValidatorPlugin
import org.scalatest.{AsyncFunSuite, BeforeAndAfterAll}

import scala.concurrent.Future

trait DefaultAmfInitialization extends AsyncFunSuite with BeforeAndAfterAll {
  private def init(): Future[Unit] = {
    amf.core.AMF.init().map { _ =>
      AMFValidatorPlugin.init()
      amf.core.AMF.registerPlugin(AMFValidatorPlugin)
    }
  }

  override protected def beforeAll(): Unit = init()
}
