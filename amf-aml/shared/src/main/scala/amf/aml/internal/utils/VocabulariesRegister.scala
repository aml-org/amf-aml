package amf.aml.internal.utils

import amf.aml.client.platform.model.document._
import amf.aml.client.platform.model.domain._
import amf.aml.client.scala.model.{document, domain}
import amf.aml.internal.metamodel.document._
import amf.aml.internal.metamodel.domain._
import amf.core.internal.convert.CoreRegister
import amf.core.internal.metamodel.Obj
import amf.core.internal.remote.Platform
import amf.core.internal.unsafe.PlatformSecrets

object VocabulariesRegister extends PlatformSecrets {

  // TODO ARM remove when APIMF-3000 is done
  def register(): Unit = register(platform)

  def register(platform: Platform): Unit = {
    CoreRegister.register(platform)

    val p: (Obj) => Boolean = (x: Obj) => x.isInstanceOf[DialectDomainElementModel]
    platform.registerWrapperPredicate(p) {
      case m: domain.DialectDomainElement => DialectDomainElement(m)
    }

    platform.registerWrapper(ClassTermModel) {
      case s: domain.ClassTerm => ClassTerm(s)
    }
    platform.registerWrapper(ExternalModel) {
      case s: domain.External => External(s)
    }
    platform.registerWrapper(NodeMappingModel) {
      case s: domain.NodeMapping => NodeMapping(s)
    }
    platform.registerWrapper(PropertyMappingModel) {
      case s: domain.PropertyMapping => PropertyMapping(s)
    }
    platform.registerWrapper(ObjectPropertyTermModel) {
      case s: domain.ObjectPropertyTerm => ObjectPropertyTerm(s)
    }
    platform.registerWrapper(UnionNodeMappingModel) {
      case s: domain.UnionNodeMapping => UnionNodeMapping(s)
    }
    platform.registerWrapper(DatatypePropertyTermModel) {
      case s: domain.DatatypePropertyTerm => DatatypePropertyTerm(s)
    }
    platform.registerWrapper(PublicNodeMappingModel) {
      case s: domain.PublicNodeMapping => PublicNodeMapping(s)
    }
    platform.registerWrapper(DocumentMappingModel) {
      case s: domain.DocumentMapping => DocumentMapping(s)
    }
    platform.registerWrapper(DocumentsModelModel) {
      case s: domain.DocumentsModel => DocumentsModel(s)
    }
    platform.registerWrapper(VocabularyReferenceModel) {
      case s: domain.VocabularyReference => VocabularyReference(s)
    }
    platform.registerWrapper(AnnotationMappingModel) {
      case s: domain.AnnotationMapping => AnnotationMapping(s)
    }
    platform.registerWrapper(SemanticExtensionModel) {
      case s: domain.SemanticExtension => SemanticExtension(s)
    }
    platform.registerWrapper(VocabularyModel) {
      case s: document.Vocabulary => new Vocabulary(s)
    }
    platform.registerWrapper(DialectModel) {
      case s: document.Dialect => Dialect(s)
    }
    platform.registerWrapper(DialectFragmentModel) {
      case s: document.DialectFragment => new DialectFragment(s)
    }
    platform.registerWrapper(DialectLibraryModel) {
      case s: document.DialectLibrary => new DialectLibrary(s)
    }
    platform.registerWrapper(DialectInstanceModel) {
      case s: document.DialectInstance => new DialectInstance(s)
    }
    platform.registerWrapper(DialectInstancePatchModel) {
      case s: document.DialectInstancePatch => new DialectInstancePatch(s)
    }
    platform.registerWrapper(DialectInstanceFragmentModel) {
      case s: document.DialectInstanceFragment => new DialectInstanceFragment(s)
    }
    platform.registerWrapper(DialectInstanceLibraryModel) {
      case s: document.DialectInstanceLibrary => new DialectInstanceLibrary(s)
    }
    platform.registerWrapper(DialectInstanceProcessingDataModel) {
      case s: document.DialectInstanceProcessingData => DialectInstanceProcessingData(s)
    }
  }
}
