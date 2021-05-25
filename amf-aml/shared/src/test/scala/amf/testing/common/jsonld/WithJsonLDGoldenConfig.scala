package amf.testing.common.jsonld

import amf.client.environment.AMLConfiguration
import amf.client.remod.amfcore.config.RenderOptions
import amf.plugins.document.graph.JsonLdDocumentForm

case class WithJsonLDGoldenConfig(golden: String, renderOptions: RenderOptions, jsonLdDocumentForm: JsonLdDocumentForm)
    extends WithJsonLDConfig(jsonLdDocumentForm) {
  def config: AMLConfiguration = AMLConfiguration.predefined().withRenderOptions(renderOptions)
}
