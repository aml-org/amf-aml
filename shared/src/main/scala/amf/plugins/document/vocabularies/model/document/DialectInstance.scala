package amf.plugins.document.vocabularies.model.document

import amf.core.metamodel.Obj
import amf.core.model.StrField
import amf.core.model.document.{BaseUnit, DeclaresModel, EncodesModel}
import amf.core.model.domain.{AmfObject, DomainElement, Linkable}
import amf.core.parser.{Annotations, ErrorHandler, Fields}
import amf.core.unsafe.PlatformSecrets
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.metamodel.document.DialectInstanceModel._
import amf.plugins.document.vocabularies.metamodel.document.{DialectInstanceFragmentModel, DialectInstanceLibraryModel, DialectInstanceModel, DialectInstancePatchModel}
import amf.plugins.document.vocabularies.model.domain.DialectDomainElement
import amf.plugins.document.vocabularies.metamodel.document.{DialectInstanceFragmentModel, DialectInstanceLibraryModel, DialectInstanceModel, DialectInstancePatchModel}
import amf.plugins.document.vocabularies.model.domain.{DialectDomainElement, External}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

trait ComposedInstancesSupport {
  var composedDialects: Map[String, Dialect] = Map()

  def dialectForComposedUnit(dialect: Dialect): Unit = composedDialects += (dialect.id -> dialect)
}

case class DialectInstance(fields: Fields, annotations: Annotations)
    extends BaseUnit
    with ExternalContext[DialectInstance]
    with DeclaresModel
    with EncodesModel
    with ComposedInstancesSupport
    with PlatformSecrets {

  override def meta: Obj = DialectInstanceModel

  def encodes: DomainElement           = fields.field(Encodes)
  def references: Seq[BaseUnit]        = fields.field(References)
  def graphDependencies: Seq[StrField] = fields.field(GraphDependencies)
  def definedBy(): StrField            = fields.field(DefinedBy)
  def declares: Seq[DomainElement]     = fields.field(Declares)

  override def componentId: String = ""

  def withDefinedBy(dialectId: String): DialectInstance        = set(DefinedBy, dialectId)
  def withGraphDependencies(ids: Seq[String]): DialectInstance = set(GraphDependencies, ids)

  override def findById(id: String, cycles: Set[String]): Option[DomainElement] = {
    AMLPlugin.registry.dialectFor(this) match {
      case Some(dialect) =>
        if (dialect.documents().selfEncoded().value()) { // avoid top level cycle
          val predicate = { element: DomainElement =>
            element.id == id
          }
          findModelByCondition(predicate, encodes, first = true, ListBuffer.empty, mutable.Set.empty).headOption.orElse(
            findInDeclaredModel(predicate, this, first = true, ListBuffer.empty, cycles).headOption.orElse(
              findInReferencedModels(id, this.references, cycles).headOption
            )
          )
        } else {
          super.findById(id, cycles)
        }
      case _ =>
        super.findById(id, cycles)
    }
  }
  override def transform(selector: DomainElement => Boolean,
                         transformation: (DomainElement, Boolean) => Option[DomainElement])(
      implicit errorHandler: ErrorHandler): BaseUnit = {
    val domainElementAdapter = (o: AmfObject) => {
      o match {
        case e: DomainElement => selector(e)
        case _                => false
      }
    }
    val transformationAdapter = (o: AmfObject, isCycle: Boolean) => {
      o match {
        case e: DomainElement => transformation(e, isCycle)
        case _                => Some(o)
      }
    }
    transformByCondition(this,
                         domainElementAdapter,
                         transformationAdapter,
                         cycleRecoverer = defaultCycleRecoverer(errorHandler))
    this
  }

}

object DialectInstance {
  def apply(): DialectInstance = apply(Annotations())

  def apply(annotations: Annotations): DialectInstance = DialectInstance(Fields(), annotations)
}

case class DialectInstanceFragment(fields: Fields, annotations: Annotations)
    extends BaseUnit
    with ExternalContext[DialectInstanceFragment]
    with EncodesModel
    with ComposedInstancesSupport {
  override def meta: Obj = DialectInstanceFragmentModel

  def references: Seq[BaseUnit]      = fields(References)
  def graphDependencies: Seq[String] = fields(GraphDependencies)
  def encodes: DomainElement         = fields(Encodes)
  def definedBy(): String            = fields(DefinedBy)

  override def componentId: String = ""

  def withDefinedBy(dialectId: String): DialectInstanceFragment     = set(DefinedBy, dialectId)
  def withGraphDepencies(ids: Seq[String]): DialectInstanceFragment = set(GraphDependencies, ids)
}

object DialectInstanceFragment {
  def apply(): DialectInstanceFragment                         = apply(Annotations())
  def apply(annotations: Annotations): DialectInstanceFragment = DialectInstanceFragment(Fields(), annotations)
}

case class DialectInstanceLibrary(fields: Fields, annotations: Annotations)
    extends BaseUnit
    with ExternalContext[DialectInstanceLibrary]
    with DeclaresModel
    with ComposedInstancesSupport {
  override def meta: Obj = DialectInstanceLibraryModel

  def references: Seq[BaseUnit]        = fields(References)
  def graphDependencies: Seq[StrField] = fields(GraphDependencies)
  def declares: Seq[DomainElement]     = fields(Declares)
  def definedBy(): StrField            = fields(DefinedBy)

  override def componentId: String = ""

  def withDefinedBy(dialectId: String): DialectInstanceLibrary        = set(DefinedBy, dialectId)
  def withGraphDependencies(ids: Seq[String]): DialectInstanceLibrary = set(GraphDependencies, ids)
}

object DialectInstanceLibrary {
  def apply(): DialectInstanceLibrary                         = apply(Annotations())
  def apply(annotations: Annotations): DialectInstanceLibrary = DialectInstanceLibrary(Fields(), annotations)
}

case class DialectInstancePatch(fields: Fields, annotations: Annotations)
    extends BaseUnit
    with ExternalContext[DialectInstancePatch]
    with DeclaresModel
    with EncodesModel {

  override def meta: Obj = DialectInstancePatchModel

  def references: Seq[BaseUnit]        = fields.field(References)
  def graphDependencies: Seq[StrField] = fields.field(GraphDependencies)
  def declares: Seq[DomainElement]     = fields.field(Declares)
  def definedBy(): StrField            = fields.field(DefinedBy)
  def extendsModel: StrField           = fields.field(DialectInstancePatchModel.Extends)
  override def encodes: DomainElement  = fields.field(Encodes)

  override def componentId: String = ""

  def withDefinedBy(dialectId: String): DialectInstancePatch        = set(DefinedBy, dialectId)
  def withGraphDependencies(ids: Seq[String]): DialectInstancePatch = set(GraphDependencies, ids)
  def withExtendsModel(target: String): DialectInstancePatch        = set(DialectInstancePatchModel.Extends, target)
}

object DialectInstancePatch {
  def apply(): DialectInstancePatch                         = apply(Annotations())
  def apply(annotations: Annotations): DialectInstancePatch = DialectInstancePatch(Fields(), annotations)
}
