package amf.plugins.document.vocabularies.parser.instances

import amf.core.Root
import org.mulesoft.common.core._
import amf.core.parser.Annotations
import amf.plugins.document.vocabularies.model.document.DialectInstanceFragment
import amf.plugins.document.vocabularies.model.domain.{DialectDomainElement, DocumentsModel}

class DialectInstanceFragmentParser(root: Root)(implicit override val ctx: DialectInstanceContext) extends DialectInstanceParser(root) {

  def parse(name: String): Option[DialectInstanceFragment] = {
    val dialectInstanceFragment: DialectInstanceFragment = DialectInstanceFragment(Annotations(map))
      .withLocation(root.location)
      .withId(root.location)
      .withDefinedBy(ctx.dialect.id)
      .withFragment(name)

    DialectInstanceReferencesParser(dialectInstanceFragment, map, root.references).parse(root.location)

    if (ctx.declarations.externals.nonEmpty)
      dialectInstanceFragment.withExternals(ctx.declarations.externals.values.toSeq)

    parseEncodedFragment(dialectInstanceFragment) match {
      case Some(dialectDomainElement) =>
        val defaultId = encodedElementDefaultId(dialectInstanceFragment)
        dialectDomainElement.withId(defaultId)
        // registering JSON pointer
        ctx.registerJsonPointerDeclaration(root.location + "#/", dialectDomainElement)

        Some(dialectInstanceFragment.withEncodes(dialectDomainElement))
      case _ => None
    }
  }

  private def parseEncodedFragment(dialectInstanceFragment: DialectInstanceFragment): Option[DialectDomainElement] = {
    Option(ctx.dialect.documents()) flatMap { documents: DocumentsModel =>
      documents
        .fragments()
        .find(dm => root.parsed.comment.getOrElse("").stripSpaces.contains(dm.documentName().value())) match {
        case Some(documentMapping) =>
          ctx.findNodeMapping(documentMapping.encoded().value()) match {
            case Some(nodeMapping) =>
              val path = dialectInstanceFragment.id + "#"
              parseNode(path, path + "/", map, nodeMapping, Map(), givenAnnotations = None)
            case _ => None
          }
        case None => None
      }
    }
  }
}
