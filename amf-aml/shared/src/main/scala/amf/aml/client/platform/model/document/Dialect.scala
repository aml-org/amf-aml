package amf.aml.client.platform.model.document

import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.StrField
import amf.aml.client.platform.model.domain.{DocumentsModel, External, SemanticExtension}
import amf.core.client.platform.model.document.{BaseUnit, DeclaresModel, EncodesModel}
import amf.aml.client.scala.model.document.{Dialect => InternalDialect}
import com.github.ghik.silencer.silent

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class Dialect(private[amf] val _internal: InternalDialect) extends BaseUnit with EncodesModel with DeclaresModel {

  @JSExportTopLevel("Dialect")
  def this() = this(InternalDialect())

  def name: StrField                              = _internal.name()
  def version: StrField                           = _internal.version()
  def nameAndVersion: String                      = _internal.nameAndVersion()
  def externals: ClientList[External]             = _internal.externals.asClient
  def documents(): DocumentsModel                 = DocumentsModel(_internal.documents())
  def extensions(): ClientList[SemanticExtension] = _internal.extensions().asClient

//  @deprecated("Useless functionality", "AML 6.0.3")
  @silent
  def header: String = _internal.header

//  @deprecated("Useless functionality", "AML 6.0.3")
  @silent
  def isLibraryHeader(header: String): Boolean = _internal.isLibraryHeader(header)

//  @deprecated("Useless functionality", "AML 6.0.3")
  @silent
  def isFragmentHeader(header: String): Boolean = _internal.isFragmentHeader(header)

//  @deprecated("Useless functionality", "AML 6.0.3")
  @silent
  def libraryHeader: ClientOption[String] = _internal.libraryHeader.asClient

//  @deprecated("Useless functionality", "AML 6.0.3")
  @silent
  def fragmentHeaders: ClientList[String] = _internal.fragmentHeaders.asClient

//  @deprecated("Useless functionality", "AML 6.0.3")
  @silent
  def allHeaders: ClientList[String] = _internal.allHeaders.asClient

  def withName(name: String): Dialect = {
    _internal.withName(name)
    this
  }

  def withVersion(version: String): Dialect = {
    _internal.withVersion(version)
    this
  }

  def withExternals(externals: ClientList[External]): Dialect = {
    _internal.withExternals(externals.asInternal)
    this
  }

  def withDocuments(documentsMapping: DocumentsModel): Dialect = {
    _internal.withDocuments(documentsMapping._internal)
    this
  }
}
