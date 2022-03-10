package amf.aml.internal.transform.steps

import amf.core.internal.annotations.Aliases
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel}
import amf.aml.internal.metamodel.domain.NodeMappingModel
import amf.aml.client.scala.model.document.{Dialect, DialectFragment, DialectLibrary}
import amf.aml.client.scala.model.domain.{
  AnnotationMapping,
  ConditionalNodeMapping,
  External,
  HasObjectRange,
  NodeMappable,
  NodeMapping,
  UnionNodeMapping
}
import amf.aml.internal.utils.AmlExtensionSyntax._
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.transform.TransformationStep
import org.mulesoft.common.collections.FilterType

import scala.collection.mutable

class DialectReferencesResolutionStage() extends TransformationStep() {
  type NodeMappable = NodeMappable.AnyNodeMappable

  override def transform(model: BaseUnit,
                         errorHandler: AMFErrorHandler,
                         configuration: AMFGraphConfiguration): BaseUnit = {
    val finalDeclarationsMap = mutable.Map[String, NodeMappable]()
    val unitDeclarations     = model.asInstanceOf[DeclaresModel].declares.filterType[NodeMappable]

    iteratePending(pending = unitDeclarations,
                   alreadyResolved = finalDeclarationsMap,
                   allDeclarations = model.recursivelyFindDeclarations())

    linkExtendedNodes(finalDeclarationsMap)

    val finalDeclarations = finalDeclarationsMap.values.toSeq

    val finalExternals: Seq[External] = model
      .recursivelyFindExternals()
      .fixAliasCollisions
      .map { external =>
        external.withId(model.location().getOrElse(model.id) + s"#/external/${external.alias.value()}")
      }

    val resolved = model match {
      case dialect: Dialect =>
        Dialect()
          .withId(dialect.id)
          .withLocation(dialect.location().getOrElse(dialect.id))
          .withDocuments(dialect.documents())
          .withDeclares(finalDeclarations)
          .withExternals(finalExternals)
          .withName(dialect.name().value())
          .withVersion(dialect.version().value())
          .withExtensions(dialect.extensions())
      case library: DialectLibrary =>
        DialectLibrary()
          .withId(library.id)
          .withLocation(library.location().getOrElse(library.id))
          .withDeclares(finalDeclarations)
          .withExternals(finalExternals)
      case fragment: DialectFragment =>
        DialectFragment()
          .withId(fragment.id)
          .withLocation(fragment.location().getOrElse(fragment.id))
          .withEncodes(fragment.encodes)
          .withExternals(finalExternals)
    }

    model.annotations.find(classOf[Aliases]).map { aliases =>
      resolved.annotations += aliases
    }

    resolved
  }

  def iteratePending(pending: Seq[NodeMappable],
                     alreadyResolved: mutable.Map[String, NodeMappable],
                     allDeclarations: Map[String, NodeMappable]): Unit = {
    if (pending.nonEmpty) {
      (pending.head, pending.tail) match {
        case (head, tail) if alreadyResolved.contains(head.id) =>
          iteratePending(tail, alreadyResolved, allDeclarations)
        case (head, tail) =>
          val resolved = resolveNodeMappable(head)
          setName(resolved, allDeclarations)
          val collectedRefs = collectReferencesFrom(resolved, allDeclarations)
          alreadyResolved += (resolved.id -> resolved)
          iteratePending(tail ++ collectedRefs, alreadyResolved, allDeclarations)
      }
    }
  }

  private def resolveNodeMappable(nodeMappable: NodeMappable): NodeMappable = {
    if (nodeMappable.isLink) {
      // if this is a link, we clone
      nodeMappable
        .effectiveLinkTarget()
        .asInstanceOf[NodeMappable]
        .copyMapping
        .withName(nodeMappable.name.value())
        .withId(nodeMappable.id)
    } else {
      // otherwise we just introduce the node mapping
      nodeMappable
    }
  }

  private def setName(nodeMappable: NodeMappable, allDeclarations: Map[String, NodeMappable]) = {
    def genName(baseName: String, allDeclarations: Map[String, NodeMappable]): String = {
      var c   = 1
      var acc = baseName
      while (allDeclarations.contains(acc)) {
        c += 1
        acc = s"$baseName$c"
      }
      acc
    }

    if (nodeMappable.name
          .value()
          .contains(".")) { // this might come from a library TODO: check collisions in names
      nodeMappable.withName(genName(nodeMappable.name.value().split(".").last, allDeclarations))
    }
  }

  private def collectReferencesFrom(nodeMappable: NodeMappable,
                                    allDeclarations: Map[String, NodeMappable]): Seq[NodeMappable] = {
    def collectRange(element: HasObjectRange[_]) = {
      for {
        range            <- element.objectRange()
        foundDeclaration <- allDeclarations.get(range.value())
      } yield {
        foundDeclaration
      }
    }

    val rangeReferences = nodeMappable match {
      case nodeMapping: NodeMapping =>
        // we add all object ranges to the list of pendings
        for {
          property <- nodeMapping.propertiesMapping()
          range    <- collectRange(property)
        } yield {
          range
        }
      case union: UnionNodeMapping => collectRange(union)
//      case conditional: ConditionalNodeMapping => collectRange()
      case annotation: AnnotationMapping => collectRange(annotation)
    }

    val extendsReferenceOption = nodeMappable.extend.headOption match {
      case Some(n: NodeMappable) => n.linkTarget.map(_.asInstanceOf[NodeMappable])
      case _                     => None
    }

    rangeReferences ++ extendsReferenceOption.toSeq
  }

  private def linkExtendedNodes(alreadyResolved: mutable.Map[String, NodeMappable]): Unit = {
    alreadyResolved.values.foreach { nodeMappable =>
      nodeMappable.extend.headOption match {
        case Some(extended: NodeMappable)
            if extended.linkTarget.isDefined && alreadyResolved.contains(extended.linkTarget.get.id) =>
          val found = alreadyResolved(extended.linkTarget.get.id)
          nodeMappable.setArrayWithoutId(NodeMappingModel.Extends, Seq(found))
        case _ =>
        // ignore
      }
    }
  }

}
