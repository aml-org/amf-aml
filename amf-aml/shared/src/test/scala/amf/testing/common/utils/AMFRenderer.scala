package amf.testing.common.utils

import amf.core.AMFSerializer
import amf.core.emitter.RenderOptions
import amf.core.model.document.BaseUnit
import amf.core.remote.Syntax.{Json, Syntax}
import amf.core.remote._

import scala.concurrent.{ExecutionContext, Future}

// TODO: this is only here for compatibility with the test suite
class AMFRenderer(unit: BaseUnit, vendor: Vendor, options: RenderOptions, syntax: Option[Syntax]) {

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

    new AMFSerializer(unit, mediaType, vendor.name, options).renderToString
  }
}

object AMFRenderer {
  def apply(unit: BaseUnit, vendor: Vendor, options: RenderOptions, syntax: Option[Syntax] = None): AMFRenderer =
    new AMFRenderer(unit, vendor, options, syntax)
}
