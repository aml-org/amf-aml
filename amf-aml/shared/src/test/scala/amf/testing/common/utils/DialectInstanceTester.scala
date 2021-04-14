package amf.testing.common.utils

import amf.client.parse.DefaultParserErrorHandler
import amf.core.emitter.RenderOptions
import amf.core.remote.{Aml, Hint, Vendor}
import amf.core.{AMFCompiler, CompilerContextBuilder}
import amf.testing.common.cycling.FunSuiteCycleTests
import org.scalatest.Assertion

import scala.concurrent.Future

trait DialectInstanceTester extends DefaultAMLInitialization { this: FunSuiteCycleTests =>

  protected def withDialect(dialect: String,
                            source: String,
                            golden: String,
                            hint: Hint,
                            target: Vendor,
                            directory: String = basePath,
                            renderOptions: Option[RenderOptions] = None): Future[Assertion] = {

    val context =
      new CompilerContextBuilder(s"file://$directory/$dialect", platform, DefaultParserErrorHandler.withRun()).build()
    for {
      _   <- new AMFCompiler(context, None, Some(Aml.name)).build()
      res <- cycle(source, golden, hint, target, directory, renderOptions)
    } yield {
      res
    }
  }

}
