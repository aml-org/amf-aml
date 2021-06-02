package amf.testing.common.utils

import amf.client.environment.AMLConfiguration
import amf.client.remod.AMFGraphConfiguration
import amf.client.remod.amfcore.config.RenderOptions
import amf.client.remod.amfcore.plugins.render.DefaultRenderConfiguration
import amf.core.AMFSerializer
import amf.core.model.document.BaseUnit
import amf.core.remote.Syntax.{Json, Syntax}
import amf.core.remote._

import scala.concurrent.{ExecutionContext, Future}

// TODO: this is only here for compatibility with the test suite
class AMFRenderer(unit: BaseUnit, vendor: Vendor, config: AMFGraphConfiguration, syntax: Option[Syntax]) {

  /** Print ast to string. */
  def renderToString(implicit executionContext: ExecutionContext): Future[String] = render()

  /** Print ast to file. */
  def renderToFile(remote: Platform, path: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    render().flatMap(s => remote.write(path, s))

  private def render()(implicit executionContext: ExecutionContext): Future[String] = {
    val mediaType = syntax.fold(vendor match {
      case Amf => "application/ld+json"
      case Aml => "application/yaml"
      case _   => "text/plain"
    })({
      case Json => "application/json"
      case _    => "application/yaml"
    })

    new AMFSerializer(unit, vendor.mediaType, DefaultRenderConfiguration(config)).renderToString
  }
}

object AMFRenderer {
  def apply(unit: BaseUnit,
            vendor: Vendor,
            config: AMFGraphConfiguration,
            syntax: Option[Syntax] = None): AMFRenderer =
    new AMFRenderer(unit, vendor, config, syntax)
}
