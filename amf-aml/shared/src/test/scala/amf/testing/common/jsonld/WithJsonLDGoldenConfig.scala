package amf.testing.common.jsonld

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.core.internal.plugins.document.graph.JsonLdDocumentForm

case class WithJsonLDGoldenConfig(golden: String, renderOptions: RenderOptions, jsonLdDocumentForm: JsonLdDocumentForm)
    extends WithJsonLDConfig(jsonLdDocumentForm) {
  def config: AMLConfiguration = AMLConfiguration.predefined().withRenderOptions(renderOptions)
}
