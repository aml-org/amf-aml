package amf.testing.common.jsonld

import amf.core.client.scala.config.RenderOptions
import amf.core.internal.plugins.document.graph.JsonLdDocumentForm

case class WithJsonLDGoldenAndSourceConfig(
    source: String,
    golden: String,
    renderOptions: RenderOptions,
    jsonLdDocumentForm: JsonLdDocumentForm
) extends WithJsonLDConfig(jsonLdDocumentForm)
