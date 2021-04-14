package amf.testing.common.jsonld

import amf.plugins.document.graph.JsonLdDocumentForm

case class WithJsonLDSourceConfig(source: String, jsonLdDocumentForm: JsonLdDocumentForm)
    extends WithJsonLDConfig(jsonLdDocumentForm)
