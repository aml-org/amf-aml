package amf.testing.common.utils

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.core.internal.plugins.render.DefaultRenderConfiguration
import amf.core.internal.render.AMFSerializer
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.remote.Mimes._
import amf.core.internal.remote.Syntax.{Json, Syntax}
import amf.core.internal.remote._

import scala.concurrent.{ExecutionContext, Future}

// TODO: this is only here for compatibility with the test suite
class AMFRenderer(unit: BaseUnit, config: AMFGraphConfiguration, syntax: Option[Syntax]) {

  /** Print ast to string. */
  def renderToString(implicit executionContext: ExecutionContext): String = render()

  /** Print ast to file. */
  def renderToFile(remote: Platform, path: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    remote.write(path, render())

  private def render()(implicit executionContext: ExecutionContext): String = {
    new AMFSerializer(unit, DefaultRenderConfiguration(config), syntax.map(_.mediaType)).renderToString
  }
}

object AMFRenderer {
  def apply(unit: BaseUnit, config: AMFGraphConfiguration, syntax: Option[Syntax] = None): AMFRenderer =
    new AMFRenderer(unit, config, syntax)
}
