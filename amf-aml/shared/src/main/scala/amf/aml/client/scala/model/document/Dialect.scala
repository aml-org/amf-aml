package amf.aml.client.scala.model.document

import amf.aml.client.scala.model.document.kind.DialectInstanceDocumentKind
import amf.aml.client.scala.model.domain.{AnnotationMapping, DocumentsModel, SemanticExtension}
import amf.aml.internal.metamodel.document.DialectModel
import amf.aml.internal.metamodel.document.DialectModel._
import amf.aml.internal.semantic.SemanticExtensionHelper
import amf.core.client.common.validation.ProfileName
import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel, EncodesModel}
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.metamodel.Type
import amf.core.internal.metamodel.document.DocumentModel.Encodes
import amf.core.internal.metamodel.document.ModuleModel.{Declares, References}
import amf.core.internal.parser.domain.{Annotations, Fields}
import org.mulesoft.common.collections._
import org.mulesoft.common.core._

case class Dialect(fields: Fields, annotations: Annotations)
    extends BaseUnit
    with ExternalContext[Dialect]
    with DeclaresModel
    with EncodesModel
    with MappingDeclarer {

  def references: Seq[BaseUnit]            = fields.field(References)
  def encodes: DomainElement               = fields.field(Encodes)
  def declares: Seq[DomainElement]         = fields.field(Declares)
  def name(): StrField                     = fields.field(Name)
  def version(): StrField                  = fields.field(Version)
  def documents(): DocumentsModel          = fields.field(Documents)
  def extensions(): Seq[SemanticExtension] = fields.field(Extensions)
  def annotationMappings(): Seq[AnnotationMapping] = declares.collect { case mapping: AnnotationMapping =>
    mapping
  }

  def nameAndVersion(): String = s"${name().value()} ${version().value()}"

  @deprecated("Useless functionality", "AML 6.0.3")
  def header: String = s"%${nameAndVersion()}".stripSpaces

  override protected[amf] def profileName: Option[ProfileName] = Some(ProfileName(nameAndVersion()))

  override def componentId: String = ""

  def withName(name: String): Dialect                             = set(Name, name)
  def withVersion(version: String): Dialect                       = set(Version, version)
  def withDocuments(documentsMapping: DocumentsModel): Dialect    = set(Documents, documentsMapping)
  def withExtensions(extensions: Seq[SemanticExtension]): Dialect = setArrayWithoutId(Extensions, extensions)

  @deprecated("Useless functionality", "AML 6.0.3")
  def libraryHeader: Option[String] =
    Option(documents()).map(d => Option(d.library())).map(_ => s"%Library/${header.stripPrefix("%")}")

  @deprecated("Useless functionality", "AML 6.0.3")
  def patchHeader: String = s"%Patch/${header.stripPrefix("%")}"

  @deprecated("Useless functionality", "AML 6.0.3")
  def isLibraryHeader(h: String): Boolean = libraryHeader.contains(h.stripSpaces)

  @deprecated("Useless functionality", "AML 6.0.3")
  def isPatchHeader(h: String): Boolean = patchHeader == h.stripSpaces

  @deprecated("Useless functionality", "AML 6.0.3")
  def isInstanceHeader(h: String): Boolean = header == h.stripSpaces

  @deprecated("Useless functionality", "AML 6.0.3")
  def fragmentHeaders: Seq[String] =
    Option(documents())
      .map(_.fragments().map(f => s"%${f.documentName().value().stripSpaces}/${header.stripPrefix("%")}"))
      .getOrElse(Seq.empty)

  @deprecated("Useless functionality", "AML 6.0.3")
  def isFragmentHeader(h: String): Boolean = fragmentHeaders.contains(h.stripSpaces)

  def hasValidHeader: Boolean = !name().isNullOrEmpty && !version().isNullOrEmpty

  @deprecated("Useless functionality", "AML 6.0.3")
  def allHeaders: Seq[String] = Seq(header) ++ libraryHeader ++ fragmentHeaders ++ Seq(patchHeader)

  def meta: DialectModel.type = DialectModel

  @deprecated("Use amf.aml.internal.parse.hints.DialectInstanceGuess.from instead", "AML 6.0.3")
  def documentKindFor(header: String): Option[DialectInstanceDocumentKind] = {
    header match {
      case h if isLibraryHeader(h)  => Some(kind.DialectInstanceLibrary)
      case h if isPatchHeader(h)    => Some(kind.DialectInstancePatch)
      case h if isFragmentHeader(h) => Some(kind.DialectInstanceFragment)
      case h if isInstanceHeader(h) => Some(kind.DialectInstance)
      case _                        => None
    }
  }

  @deprecated("Useless functionality", "AML 6.0.3")
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

  private[amf] def hasExtensions(): Boolean = this.extensions().nonEmpty

  private[amf] def usesHeaderMatching: Boolean = !usesKeyPropertyMatching
  private[amf] def extensionIndex: Map[String, Dialect] =
    extensions().map(e => e.extensionName().value() -> this).toMap
  private[amf] def extensionModels: Map[String, Map[String, Type]] = {
    extensions()
      .flatMap { semantic =>
        val annotation = SemanticExtensionHelper.findAnnotationMapping(this, semantic)
        annotation.domain().map { domain =>
          domain.value() -> (annotation.nodePropertyMapping().value() -> annotation.toField().`type`)
        }
      }
      .groupBy(x => x._1)
      .mapValues(values =>
        values.map { x =>
          x._2
        }.toMap
      )
  }
}

object Dialect {
  def apply(): Dialect = apply(Annotations())

  def apply(annotations: Annotations): Dialect = Dialect(Fields(), annotations)
}
