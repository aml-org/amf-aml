package amf.testing.common.utils

import amf.core.emitter.RenderOptions
import amf.core.remote.{Hint, Vendor}
import amf.testing.common.cycling.FunSuiteCycleTests
import org.scalatest.Assertion

import scala.concurrent.Future

trait DialectInstanceTester extends DefaultAMLInitialization with DialectRegistrationHelper {
  this: FunSuiteCycleTests =>

  protected def cycleWithDialect(dialect: String,
                                 source: String,
                                 golden: String,
                                 hint: Hint,
                                 target: Vendor,
                                 directory: String = basePath,
                                 renderOptions: Option[RenderOptions] = None): Future[Assertion] = {

    withDialect(s"file://$directory/$dialect") { _ =>
      cycle(source, golden, hint, target, directory, renderOptions)
    }
  }

}
