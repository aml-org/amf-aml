package amf.testing.common.utils

import amf.core.client.scala.config.RenderOptions
import amf.core.internal.remote.{Hint, SpecId}
import amf.testing.common.cycling.FunSuiteCycleTests
import org.scalatest.Assertion

import scala.concurrent.Future

trait DialectInstanceTester extends DialectRegistrationHelper { this: FunSuiteCycleTests =>

  protected def cycleWithDialect(dialect: String,
                                 source: String,
                                 golden: String,
                                 hint: Hint,
                                 target: SpecId,
                                 directory: String = basePath,
                                 renderOptions: Option[RenderOptions] = None): Future[Assertion] = {
    withDialect(s"file://$directory/$dialect") { (_, config) =>
      val configuration =
        renderOptions.fold(
            config.withRenderOptions(RenderOptions().withSourceMaps.withPrettyPrint.withoutFlattenedJsonLd))(r =>
          config.withRenderOptions(r))
      cycle(source, golden, hint, target, directory, configuration)
    }
  }

}
