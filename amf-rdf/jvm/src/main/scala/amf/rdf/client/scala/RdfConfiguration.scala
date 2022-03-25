package amf.rdf.client.scala

import amf.core.client.scala.AMFGraphConfiguration
import amf.rdf.internal.plugins.{RdfParsePlugin, RdfRenderPlugin, RdfSyntaxParsePlugin, RdfSyntaxRenderPlugin}

object RdfConfiguration {
  def apply(): AMFGraphConfiguration = {
    AMFGraphConfiguration
      .predefined()
      .emptyPlugins()
      .withPlugins(List(RdfParsePlugin, RdfRenderPlugin, RdfSyntaxParsePlugin, RdfSyntaxRenderPlugin))
  }
}
