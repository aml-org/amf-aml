package amf.aml.internal.convert

import amf.aml.client.platform
import amf.aml.client.platform.model.document
import amf.aml.client.platform.model.document.{
  Dialect => ClientDialect,
  DialectInstance => ClientDialectInstance,
  Vocabulary => ClientVocabulary
}
import amf.aml.client.platform.model.domain.{
  AnnotationMapping => ClientAnnotationMapping,
  ClassTerm => ClientClassTerm,
  DatatypePropertyTerm => ClientDatatypePropertyTerm,
  DialectDomainElement => ClientDialectDomainElement,
  DocumentMapping => ClientDocumentMapping,
  DocumentsModel => ClientDocumentsModel,
  External => ClientExternal,
  NodeMapping => ClientNodeMapping,
  ObjectPropertyTerm => ClientObjectPropertyTerm,
  PropertyMapping => ClientPropertyMapping,
  PublicNodeMapping => ClientPublicNodeMapping,
  SemanticExtension => ClientSemanticExtension,
  VocabularyReference => ClientVocabularyReference
}
import amf.aml.client.scala.{AMLConfiguration, AMLDialectInstanceResult, AMLDialectResult, AMLVocabularyResult}
import amf.core.internal.convert.{BidirectionalMatcher, CoreBaseConverter}
import amf.core.internal.unsafe.PlatformSecrets
import amf.aml.client.scala.model.document.{Dialect, DialectInstance, DialectInstanceProcessingData, Vocabulary}
import amf.aml.client.scala.model.domain._

trait VocabulariesBaseConverter
    extends CoreBaseConverter
    with PropertyMappingConverter
    with AnnotationMappingConverter
    with SemanticExtensionConverter
    with PublicNodeMappingConverter
    with DialectConverter
    with DialectInstanceConverter
    with VocabularyConverter
    with DocumentsModelConverter
    with DocumentMappingConverter
    with VocabularyReferenceConverter
    with ExternalConverter
    with NodeMappingConverter
    with DialectDomainElementConverter
    with DatatypePropertyMappingConverter
    with ObjectPropertyMappingConverter
    with ClassTermMappingConverter
    with AMLConfigurationConverter
    with AMLDialectResultConverter
    with AMLDialectInstanceResultConverter
    with AMLVocabularyResultConverter
    with DialectInstanceProcessingDataConverter

trait DatatypePropertyMappingConverter extends PlatformSecrets {

  implicit object DatatypePropertyMappingConverter
      extends BidirectionalMatcher[DatatypePropertyTerm, ClientDatatypePropertyTerm] {
    override def asClient(from: DatatypePropertyTerm): ClientDatatypePropertyTerm =
      platform.wrap[ClientDatatypePropertyTerm](from)
    override def asInternal(from: ClientDatatypePropertyTerm): DatatypePropertyTerm = from._internal
  }
}

trait ObjectPropertyMappingConverter extends PlatformSecrets {

  implicit object ObjectPropertyMappingConverter
      extends BidirectionalMatcher[ObjectPropertyTerm, ClientObjectPropertyTerm] {
    override def asClient(from: ObjectPropertyTerm): ClientObjectPropertyTerm =
      platform.wrap[ClientObjectPropertyTerm](from)
    override def asInternal(from: ClientObjectPropertyTerm): ObjectPropertyTerm = from._internal
  }
}

trait DialectConverter extends PlatformSecrets {

  implicit object DialectConverter extends BidirectionalMatcher[Dialect, ClientDialect] {
    override def asClient(from: Dialect): ClientDialect   = ClientDialect(from)
    override def asInternal(from: ClientDialect): Dialect = from._internal
  }
}

trait DialectInstanceConverter extends PlatformSecrets {

  implicit object DialectInstanceConverter extends BidirectionalMatcher[DialectInstance, ClientDialectInstance] {
    override def asClient(from: DialectInstance): ClientDialectInstance   = new ClientDialectInstance(from)
    override def asInternal(from: ClientDialectInstance): DialectInstance = from._internal
  }
}

trait VocabularyConverter extends PlatformSecrets {

  implicit object VocabularyConverter extends BidirectionalMatcher[Vocabulary, ClientVocabulary] {
    override def asClient(from: Vocabulary): ClientVocabulary   = new ClientVocabulary(from)
    override def asInternal(from: ClientVocabulary): Vocabulary = from._internal
  }
}

trait ClassTermMappingConverter extends PlatformSecrets {

  implicit object ClassTermMappingConverter extends BidirectionalMatcher[ClassTerm, ClientClassTerm] {
    override def asClient(from: ClassTerm): ClientClassTerm   = platform.wrap[ClientClassTerm](from)
    override def asInternal(from: ClientClassTerm): ClassTerm = from._internal
  }
}

trait PropertyMappingConverter extends PlatformSecrets {

  implicit object PropertyMappingConverter extends BidirectionalMatcher[PropertyMapping, ClientPropertyMapping] {
    override def asClient(from: PropertyMapping): ClientPropertyMapping   = platform.wrap[ClientPropertyMapping](from)
    override def asInternal(from: ClientPropertyMapping): PropertyMapping = from._internal
  }
}

trait AnnotationMappingConverter extends PlatformSecrets {

  implicit object AnnotationMappingConverter extends BidirectionalMatcher[AnnotationMapping, ClientAnnotationMapping] {
    override def asClient(from: AnnotationMapping): ClientAnnotationMapping =
      platform.wrap[ClientAnnotationMapping](from)
    override def asInternal(from: ClientAnnotationMapping): AnnotationMapping = from._internal
  }
}

trait SemanticExtensionConverter extends PlatformSecrets {

  implicit object SemanticExtensionConverter extends BidirectionalMatcher[SemanticExtension, ClientSemanticExtension] {
    override def asClient(from: SemanticExtension): ClientSemanticExtension =
      platform.wrap[ClientSemanticExtension](from)
    override def asInternal(from: ClientSemanticExtension): SemanticExtension = from._internal
  }
}

trait PublicNodeMappingConverter extends PlatformSecrets {

  implicit object PublicNodeMappingConverter extends BidirectionalMatcher[PublicNodeMapping, ClientPublicNodeMapping] {
    override def asClient(from: PublicNodeMapping): ClientPublicNodeMapping =
      platform.wrap[ClientPublicNodeMapping](from)
    override def asInternal(from: ClientPublicNodeMapping): PublicNodeMapping = from._internal
  }
}

trait DocumentsModelConverter extends PlatformSecrets {

  implicit object DocumentModelConverter extends BidirectionalMatcher[DocumentsModel, ClientDocumentsModel] {
    override def asClient(from: DocumentsModel): ClientDocumentsModel   = platform.wrap[ClientDocumentsModel](from)
    override def asInternal(from: ClientDocumentsModel): DocumentsModel = from._internal
  }
}

trait DocumentMappingConverter extends PlatformSecrets {

  implicit object DocumentMappingConverter extends BidirectionalMatcher[DocumentMapping, ClientDocumentMapping] {
    override def asClient(from: DocumentMapping): ClientDocumentMapping   = platform.wrap[ClientDocumentMapping](from)
    override def asInternal(from: ClientDocumentMapping): DocumentMapping = from._internal
  }
}

trait VocabularyReferenceConverter extends PlatformSecrets {

  implicit object VocabularyReferenceConverter
      extends BidirectionalMatcher[VocabularyReference, ClientVocabularyReference] {
    override def asClient(from: VocabularyReference): ClientVocabularyReference =
      platform.wrap[ClientVocabularyReference](from)
    override def asInternal(from: ClientVocabularyReference): VocabularyReference = from._internal
  }
}

trait ExternalConverter extends PlatformSecrets {

  implicit object ExternalConverter extends BidirectionalMatcher[External, ClientExternal] {
    override def asClient(from: External): ClientExternal   = platform.wrap[ClientExternal](from)
    override def asInternal(from: ClientExternal): External = from._internal
  }
}

trait NodeMappingConverter extends PlatformSecrets {

  implicit object NodeMappingConverter extends BidirectionalMatcher[NodeMapping, ClientNodeMapping] {
    override def asClient(from: NodeMapping): ClientNodeMapping   = platform.wrap[ClientNodeMapping](from)
    override def asInternal(from: ClientNodeMapping): NodeMapping = from._internal
  }
}

trait DialectDomainElementConverter extends PlatformSecrets {

  implicit object DialectDomainElementConverter
      extends BidirectionalMatcher[DialectDomainElement, ClientDialectDomainElement] {
    override def asClient(from: DialectDomainElement): ClientDialectDomainElement =
      platform.wrap[ClientDialectDomainElement](from)
    override def asInternal(from: ClientDialectDomainElement): DialectDomainElement = from._internal
  }
}

trait AMLConfigurationConverter {
  implicit object AMLConfigurationMatcher extends BidirectionalMatcher[AMLConfiguration, platform.AMLConfiguration] {
    override def asClient(from: AMLConfiguration): platform.AMLConfiguration =
      new platform.AMLConfiguration(from)
    override def asInternal(from: platform.AMLConfiguration): AMLConfiguration = from._internal
  }
}

trait AMLDialectResultConverter {
  implicit object AMLDialectResultMatcher extends BidirectionalMatcher[AMLDialectResult, platform.AMLDialectResult] {
    override def asClient(from: AMLDialectResult): platform.AMLDialectResult =
      new platform.AMLDialectResult(from)
    override def asInternal(from: platform.AMLDialectResult): AMLDialectResult = from._internal
  }
}

trait AMLDialectInstanceResultConverter {
  implicit object AMLDialectInstanceResultMatcher
      extends BidirectionalMatcher[AMLDialectInstanceResult, platform.AMLDialectInstanceResult] {
    override def asClient(from: AMLDialectInstanceResult): platform.AMLDialectInstanceResult =
      new platform.AMLDialectInstanceResult(from)
    override def asInternal(from: platform.AMLDialectInstanceResult): AMLDialectInstanceResult = from._internal
  }
}

trait AMLVocabularyResultConverter {
  implicit object AMLVocabularyResultMatcher
      extends BidirectionalMatcher[AMLVocabularyResult, platform.AMLVocabularyResult] {
    override def asClient(from: AMLVocabularyResult): platform.AMLVocabularyResult =
      new platform.AMLVocabularyResult(from)
    override def asInternal(from: platform.AMLVocabularyResult): AMLVocabularyResult = from._internal
  }
}

trait DialectInstanceProcessingDataConverter {
  implicit object DialectInstanceProcessingDataMatcher
      extends BidirectionalMatcher[DialectInstanceProcessingData,
                                   platform.model.document.DialectInstanceProcessingData] {
    override def asClient(from: DialectInstanceProcessingData): platform.model.document.DialectInstanceProcessingData =
      new platform.model.document.DialectInstanceProcessingData(from)

    override def asInternal(
        from: platform.model.document.DialectInstanceProcessingData): DialectInstanceProcessingData = from._internal
  }
}
