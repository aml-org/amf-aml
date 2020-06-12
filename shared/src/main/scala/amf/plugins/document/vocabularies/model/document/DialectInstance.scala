package amf.plugins.document.vocabularies.model.document

import amf.core.errorhandling.ErrorHandler
import amf.core.metamodel.Obj
import amf.core.model.StrField
import amf.core.model.document.{BaseUnit, DeclaresModel, EncodesModel}
import amf.core.model.domain.{AmfObject, DomainElement}
import amf.core.parser.{Annotations, Fields}
import amf.core.traversal.{
  DomainElementSelectorAdapter,
  DomainElementTransformationAdapter,
  TransformationData,
  TransformationTraversal
}
import amf.core.unsafe.PlatformSecrets
import amf.plugins.document.vocabularies.metamodel.document.DialectInstanceFragmentModel.Fragment
import amf.plugins.document.vocabularies.metamodel.document.DialectInstanceModel._
import amf.plugins.document.vocabularies.metamodel.document.{
  DialectInstanceFragmentModel,
  DialectInstanceLibraryModel,
  DialectInstanceModel,
  DialectInstancePatchModel
}

trait ComposedInstancesSupport {
  var composedDialects: Map[String, Dialect] = Map()

  def dialectForComposedUnit(dialect: Dialect): Unit =
    composedDialects += (dialect.id -> dialect)
}

trait DialectInstanceUnit extends BaseUnit with ExternalContext[DialectInstanceUnit] {
  def references: Seq[BaseUnit]
  def graphDependencies: Seq[StrField]
  def definedBy(): StrField
}

case class DialectInstance(fields: Fields, annotations: Annotations)
    extends DialectInstanceUnit
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

  def withDefinedBy(dialectId: String): DialectInstance =
    set(DefinedBy, dialectId)

  def withGraphDependencies(ids: Seq[String]): DialectInstance =
    set(GraphDependencies, ids)

  override def transform(selector: DomainElement => Boolean,
                         transformation: (DomainElement, Boolean) => Option[DomainElement])(
      implicit errorHandler: ErrorHandler): BaseUnit = {
    val domainElementAdapter  = new DomainElementSelectorAdapter(selector)
    val transformationAdapter = new DomainElementTransformationAdapter(transformation)
    new TransformationTraversal(TransformationData(domainElementAdapter, transformationAdapter)).traverse(this)
    this
  }

}

object DialectInstance {
  def apply(): DialectInstance = apply(Annotations())

  def apply(annotations: Annotations): DialectInstance =
    DialectInstance(Fields(), annotations)
}

case class DialectInstanceFragment(fields: Fields, annotations: Annotations)
    extends DialectInstanceUnit
    with EncodesModel
    with ComposedInstancesSupport {
  override def meta: Obj = DialectInstanceFragmentModel

  def references: Seq[BaseUnit]        = fields.field(References)
  def graphDependencies: Seq[StrField] = fields.field(GraphDependencies)
  def encodes: DomainElement           = fields.field(Encodes)
  def definedBy(): StrField            = fields.field(DefinedBy)
  def fragment(): StrField             = fields.field(Fragment)

  override def componentId: String = ""

  def withDefinedBy(dialectId: String): DialectInstanceFragment =
    set(DefinedBy, dialectId)
  def withFragment(fragmentId: String): DialectInstanceFragment =
    set(Fragment, fragmentId)
  def withGraphDepencies(ids: Seq[String]): DialectInstanceFragment =
    set(GraphDependencies, ids)
}

object DialectInstanceFragment {
  def apply(): DialectInstanceFragment = apply(Annotations())
  def apply(annotations: Annotations): DialectInstanceFragment =
    DialectInstanceFragment(Fields(), annotations)
}

case class DialectInstanceLibrary(fields: Fields, annotations: Annotations)
    extends DialectInstanceUnit
    with DeclaresModel
    with ComposedInstancesSupport {
  override def meta: Obj = DialectInstanceLibraryModel

  def references: Seq[BaseUnit]        = fields.field(References)
  def graphDependencies: Seq[StrField] = fields.field(GraphDependencies)
  def declares: Seq[DomainElement]     = fields.field(Declares)
  def definedBy(): StrField            = fields.field(DefinedBy)

  override def componentId: String = ""

  def withDefinedBy(dialectId: String): DialectInstanceLibrary =
    set(DefinedBy, dialectId)
  def withGraphDependencies(ids: Seq[String]): DialectInstanceLibrary =
    set(GraphDependencies, ids)
}

object DialectInstanceLibrary {
  def apply(): DialectInstanceLibrary = apply(Annotations())
  def apply(annotations: Annotations): DialectInstanceLibrary =
    DialectInstanceLibrary(Fields(), annotations)
}

case class DialectInstancePatch(fields: Fields, annotations: Annotations)
    extends DialectInstanceUnit
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

  def withDefinedBy(dialectId: String): DialectInstancePatch =
    set(DefinedBy, dialectId)
  def withGraphDependencies(ids: Seq[String]): DialectInstancePatch =
    set(GraphDependencies, ids)
  def withExtendsModel(target: String): DialectInstancePatch =
    set(DialectInstancePatchModel.Extends, target)
}

object DialectInstancePatch {
  def apply(): DialectInstancePatch = apply(Annotations())
  def apply(annotations: Annotations): DialectInstancePatch =
    DialectInstancePatch(Fields(), annotations)
}
