package amf.plugins.document.vocabularies.parser.dialects

import amf.core.annotations.{ErrorDeclaration => DeclaredErrorDeclaration}
import amf.core.errorhandling.ErrorHandler
import amf.core.model.domain.DomainElement
import amf.core.parser.{Annotations, EmptyFutureDeclarations, Fields, FutureDeclarations, SearchScope}
import amf.plugins.document.vocabularies.metamodel.domain.{AnnotationMappingModel, NodeMappingModel}
import amf.plugins.document.vocabularies.model.domain._
import amf.plugins.document.vocabularies.parser.vocabularies.VocabularyDeclarations
import org.yaml.model.YPart

class DialectDeclarations(var nodeMappings: Map[String, NodeMappable[_]] = Map(),
                          var annotationMappings: Map[String, AnnotationMapping] = Map(),
                          errorHandler: ErrorHandler,
                          futureDeclarations: FutureDeclarations)
    extends VocabularyDeclarations(Map(), Map(), Map(), Map(), Map(), errorHandler, futureDeclarations) {

  type NodeMappable = NodeMappable.AnyNodeMappable

  /** Get or create specified library. */
  override def getOrCreateLibrary(alias: String): DialectDeclarations = {
    libraries.get(alias) match {
      case Some(lib: DialectDeclarations) => lib
      case _ =>
        val result =
          new DialectDeclarations(errorHandler = errorHandler, futureDeclarations = EmptyFutureDeclarations())
        libraries = libraries + (alias -> result)
        result
    }
  }

  def +=(annotationMapping: AnnotationMapping): DialectDeclarations = {
    annotationMappings += (annotationMapping.name().value() -> annotationMapping)
    this
  }

  def +=(nodeMapping: NodeMappable): DialectDeclarations = {
    nodeMappings += (nodeMapping.name.value() -> nodeMapping)
    if (!nodeMapping.isUnresolved) {
      futureDeclarations.resolveRef(nodeMapping.name.value(), nodeMapping)
    }
    this
  }

  def registerNodeMapping(nodeMapping: NodeMappable): DialectDeclarations = {
    nodeMappings += (nodeMapping.name.value() -> nodeMapping)
    this
  }

  def findNodeMapping(key: String, scope: SearchScope.Scope): Option[NodeMappable] =
    findForType(key, _.asInstanceOf[DialectDeclarations].nodeMappings, scope) collect {
      case nm: NodeMappable => nm
    }

  def findAnnotationMapping(key: String, scope: SearchScope.Scope): Option[AnnotationMapping] =
    findForType(key, _.asInstanceOf[DialectDeclarations].annotationMappings, scope) collect {
      case am: AnnotationMapping => am
    }

  def findNodeMappingOrError(ast: YPart)(key: String, scope: SearchScope.Scope): NodeMappable =
    findNodeMapping(key, scope) match {
      case Some(result) => result
      case _ =>
        error(s"NodeMappable $key not found", ast)
        ErrorNodeMappable(key, ast)
    }

  def findAnnotationMappingOrError(ast: YPart)(key: String, scope: SearchScope.Scope): AnnotationMapping =
    findAnnotationMapping(key, scope) match {
      case Some(result) => result
      case _ =>
        error(s"Annotation mapping $key not found", ast)
        ErrorAnnotationMapping(key, ast)
    }

  def findClassTerm(key: String, scope: SearchScope.Scope): Option[ClassTerm] =
    findForType(key, _.asInstanceOf[DialectDeclarations].classTerms, scope) match {
      case Some(ct: ClassTerm) => Some(ct)
      case _                   => resolveExternal(key).map(ClassTerm().withId(_))
    }

  def findPropertyTerm(key: String, scope: SearchScope.Scope): Option[PropertyTerm] =
    findForType(key, _.asInstanceOf[DialectDeclarations].propertyTerms, scope) match {
      case Some(pt: PropertyTerm) => Some(pt)
      case _                      => resolveExternal(key).map(DatatypePropertyTerm().withId(_))
    }

  override def declarables(): Seq[DomainElement] = nodeMappings.values.toSeq ++ annotationMappings.values.toSeq

  case class ErrorNodeMappable(idPart: String, part: YPart)
      extends NodeMapping(Fields(), Annotations(part))
      with DeclaredErrorDeclaration[NodeMappingModel.type] {
    override val namespace: String            = "http://amferror.com/#errorNodeMappable/"
    override val model: NodeMappingModel.type = NodeMappingModel

    withId(idPart)

    override def newErrorInstance: DeclaredErrorDeclaration[NodeMappingModel.type] = ErrorNodeMappable(idPart, part)
  }

  case class ErrorAnnotationMapping(idPart: String, part: YPart)
      extends AnnotationMapping(Fields(), Annotations(part))
      with DeclaredErrorDeclaration[AnnotationMappingModel.type] {
    override val namespace: String                  = "http://amferror.com/#errorAnnotationMapping/"
    override val model: AnnotationMappingModel.type = AnnotationMappingModel

    withId(idPart)

    override def newErrorInstance: DeclaredErrorDeclaration[AnnotationMappingModel.type] =
      ErrorAnnotationMapping(idPart, part)
  }
}
