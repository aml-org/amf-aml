package amf.testing.common.jsonld

import amf.core.internal.plugins.document.graph.JsonLdDocumentForm

case class WithJsonLDSourceConfig(source: String, jsonLdDocumentForm: JsonLdDocumentForm)
    extends WithJsonLDConfig(jsonLdDocumentForm)
