package amf.aml.internal.parse.instances

import amf.core.internal.parser.Root
import org.mulesoft.common.core._
import amf.core.internal.parser.domain.Annotations
import amf.aml.internal.metamodel.document.DialectInstanceModel
import amf.aml.client.scala.model.document.{DialectInstanceFragment, DialectInstanceProcessingData}
import amf.aml.client.scala.model.domain.{DialectDomainElement, DocumentsModel}
import amf.aml.internal.validate.DialectValidations.DialectError

class DialectInstanceFragmentParser(root: Root)(implicit override val ctx: DialectInstanceContext)
    extends DialectInstanceParser(root) {

  def parse(name: String): DialectInstanceFragment = {
    val dialectInstanceFragment: DialectInstanceFragment = DialectInstanceFragment(Annotations(map))
      .withLocation(root.location)
      .withId(root.location)
      .withProcessingData(DialectInstanceProcessingData().withTransformed(false).withDefinedBy(ctx.dialect.id))
      .withFragment(name)

    DialectInstanceReferencesParser(dialectInstanceFragment, map, root.references).parse(root.location)

    if (ctx.declarations.externals.nonEmpty)
      dialectInstanceFragment.withExternals(ctx.declarations.externals.values.toSeq)

    val dialectDomainElement = parseEncodedFragment(dialectInstanceFragment, name)
    val defaultId            = encodedElementDefaultId(dialectInstanceFragment)
    dialectDomainElement.withId(defaultId)
    // registering JSON pointer
    ctx.registerJsonPointerDeclaration(root.location + "#/", dialectDomainElement)

    dialectInstanceFragment.set(DialectInstanceModel.Encodes, dialectDomainElement, Annotations.inferred())
  }

  private def parseEncodedFragment(dialectInstanceFragment: DialectInstanceFragment,
                                   fragmentName: String): DialectDomainElement = {
    val documentMapping = Option(ctx.dialect.documents()).flatMap { documents =>
      documents.fragments().find(fragmentName == _.documentName().value())
    }
    documentMapping match {
      case Some(documentMapping) =>
        ctx.findNodeMapping(documentMapping.encoded().value()) match {
          case Some(nodeMapping) =>
            val path = dialectInstanceFragment.id + "#"
            parseNode(path, path + "/", map, nodeMapping, Map(), givenAnnotations = None)
          case _ =>
            emptyElementWithViolation(s"Could not find node mapping for: ${documentMapping.encoded().value()}")
        }
      case None =>
        emptyElementWithViolation("Could not obtain fragment document mapping from dialect")
    }
  }
}
