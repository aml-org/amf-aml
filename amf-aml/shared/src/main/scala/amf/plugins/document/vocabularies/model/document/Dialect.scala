package amf.plugins.document.vocabularies.model.document

import amf.core.model.StrField
import amf.core.model.document.{BaseUnit, DeclaresModel, EncodesModel}
import amf.core.model.domain.DomainElement
import amf.core.parser.{Annotations, Fields}
import amf.plugins.document.vocabularies.metamodel.document.DialectModel
import amf.plugins.document.vocabularies.metamodel.document.DialectModel._
import amf.plugins.document.vocabularies.model.document.kind.DialectInstanceDocumentKind
import amf.plugins.document.vocabularies.model.domain.DocumentsModel
import org.mulesoft.common.core._

case class Dialect(fields: Fields, annotations: Annotations)
    extends BaseUnit
    with ExternalContext[Dialect]
    with DeclaresModel
    with EncodesModel
    with MappingDeclarer {

  def references: Seq[BaseUnit]    = fields.field(References)
  def encodes: DomainElement       = fields.field(Encodes)
  def declares: Seq[DomainElement] = fields.field(Declares)
  def name(): StrField             = fields.field(Name)
  def version(): StrField          = fields.field(Version)
  def documents(): DocumentsModel  = fields.field(Documents)

  def nameAndVersion(): String = s"${name().value()} ${version().value()}"

  def header: String = s"%${nameAndVersion()}".stripSpaces

  override def componentId: String = ""

  def withName(name: String): Dialect                          = set(Name, name)
  def withVersion(version: String): Dialect                    = set(Version, version)
  def withDocuments(documentsMapping: DocumentsModel): Dialect = set(Documents, documentsMapping)

  def libraryHeader: Option[String] =
    Option(documents()).map(d => Option(d.library())).map(_ => s"%Library/${header.stripPrefix("%")}")

  def patchHeader: String = s"%Patch/${header.stripPrefix("%")}"

  def isLibraryHeader(h: String): Boolean = libraryHeader.contains(h.stripSpaces)

  def isPatchHeader(h: String): Boolean = patchHeader == h.stripSpaces

  def isInstanceHeader(h: String): Boolean = header == h.stripSpaces

  def fragmentHeaders: Seq[String] =
    Option(documents())
      .map(_.fragments().map(f => s"%${f.documentName().value().stripSpaces}/${header.stripPrefix("%")}"))
      .getOrElse(Seq.empty)

  def isFragmentHeader(h: String): Boolean = fragmentHeaders.contains(h.stripSpaces)

  def hasValidHeader: Boolean = !name().isNullOrEmpty && !version().isNullOrEmpty

  def allHeaders: Seq[String] = Seq(header) ++ libraryHeader ++ fragmentHeaders ++ Seq(patchHeader)

  def meta: DialectModel.type = DialectModel

  def documentKindFor(header: String): Option[DialectInstanceDocumentKind] = {
    header match {
      case h if isLibraryHeader(h)  => Some(kind.DialectInstanceLibrary)
      case h if isPatchHeader(h)    => Some(kind.DialectInstancePatch)
      case h if isFragmentHeader(h) => Some(kind.DialectInstanceFragment)
      case h if isInstanceHeader(h) => Some(kind.DialectInstance)
      case _                        => None
    }
  }

  def acceptsHeader(header: String): Boolean = documentKindFor(header).isDefined

  private[amf] def usesKeyPropertyMatching: Boolean = {
    val usesKeyProperty = for {
      documentsModel  <- Option(documents())
      usesKeyProperty <- documentsModel.keyProperty().option()
    } yield {
      usesKeyProperty
    }
    usesKeyProperty.contains(true)
  }

  private[amf] def usesHeaderMatching: Boolean = !usesKeyPropertyMatching
}

object Dialect {
  def apply(): Dialect = apply(Annotations())

  def apply(annotations: Annotations): Dialect = Dialect(Fields(), annotations)
}
