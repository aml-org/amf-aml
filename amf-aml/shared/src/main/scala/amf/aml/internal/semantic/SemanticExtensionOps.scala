package amf.aml.internal.semantic

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.AnnotationMapping
import amf.aml.internal.semantic.SemanticExtensionHelper.{findAnnotationMapping, findSemanticExtension}

object SemanticExtensionOps {

  def findExtensionMapping(name: String,
                           parentTypes: Seq[String],
                           finder: ExtensionDialectFinder): Option[(AnnotationMapping, Dialect)] = {
    findExtensionDialect(name, finder).flatMap { dialect =>
      findSemanticExtension(dialect, name)
        .map(findAnnotationMapping(dialect, _))
        .filter(_.appliesTo(parentTypes))
        .map(mapping => (mapping, dialect))
    }
  }

  private def findExtensionDialect(name: String, finder: ExtensionDialectFinder): Option[Dialect] = finder.find(name)
}
