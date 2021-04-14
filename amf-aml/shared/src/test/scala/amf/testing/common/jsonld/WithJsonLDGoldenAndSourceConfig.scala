package amf.testing.common.jsonld

import amf.core.emitter.RenderOptions
import amf.plugins.document.graph.JsonLdDocumentForm

case class WithJsonLDGoldenAndSourceConfig(source: String,
                                           golden: String,
                                           renderOptions: RenderOptions,
                                           jsonLdDocumentForm: JsonLdDocumentForm)
    extends WithJsonLDConfig(jsonLdDocumentForm)
