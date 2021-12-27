package amf.aml.internal.render.emitters.instances
import amf.core.client.common.position.Position
import amf.core.client.scala.config.RenderOptions
import amf.core.internal.annotations.Aliases.{Alias, ImportLocation, RefId}
import amf.core.internal.annotations.{LexicalInformation, SourceNode}
import amf.core.internal.render.BaseEmitters._
import amf.core.internal.render.emitters.{EntryEmitter, PartEmitter}
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.domain.DomainElementModel
import amf.core.client.scala.model.domain.extensions.DomainExtension
import amf.core.client.scala.model.domain.{AmfArray, AmfElement, AmfScalar, ScalarNode}
import amf.core.client.common.position.Position.ZERO
import amf.core.client.scala.model.DataType
import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel}
import amf.core.internal.parser.domain.{Annotations, FieldEntry, Value}
import amf.core.internal.render.SpecOrdering
import amf.aml.internal.annotations.{CustomBase, CustomId, JsonPointerRef, RefInclude}
import amf.aml.internal.metamodel.domain.{DialectDomainElementModel, NodeMappableModel}
import amf.aml.client.scala.model.document.{Dialect, DialectInstanceFragment, DialectInstanceUnit}
import amf.aml.client.scala.model.domain._
import org.mulesoft.common.time.SimpleDateTime
import org.yaml.model.YDocument.{EntryBuilder, PartBuilder}
import org.yaml.model.{YNode, YType}

import scala.language.existentials

class RootDialectNodeEmitter(node: DialectDomainElement,
                             nodeMappable: NodeMappable[_ <: NodeMappableModel],
                             instance: DialectInstanceUnit,
                             dialect: Dialect,
                             ordering: SpecOrdering,
                             keyPropertyId: Option[String] = None,
                             discriminator: Option[(String, String)] = None,
                             emitDialect: Boolean = false,
                             topLevelEmitters: Seq[EntryEmitter] = Nil,
                             renderOptions: RenderOptions)(implicit nodeMappableFinder: NodeMappableFinder)
    extends DialectNodeEmitter(node,
                               nodeMappable,
                               instance.references,
                               dialect,
                               ordering,
                               keyPropertyId,
                               discriminator,
                               emitDialect,
                               topLevelEmitters,
                               renderOptions) {

  lazy val referencesAliasIndex: Map[RefId, (Alias, ImportLocation)] = buildReferenceAliasIndexFrom(instance)

  override def emitters: Seq[EntryEmitter] = {
    var emitters = super.emitters
    // in case this is the root dialect node, we look for declarations
    emitters ++= declarationsEmitters()

    // and also for use of libraries
    emitters ++= Seq(ReferencesEmitter(instance, ordering, referencesAliasIndex))
    emitters
  }

  private def declares(): Option[DeclaresModel] = instance match {
    case d: DeclaresModel => Some(d)
    case _                => None
  }

  def declarationsEmitters(): Seq[EntryEmitter] = {
    val emitters = for {
      docs  <- Option(dialect.documents())
      root  <- Option(docs.root())
      model <- declares()
    } yield {
      if (root.encoded().value() == node.id) {
        Nil
      } else {
        root.declaredNodes().foldLeft(Seq[EntryEmitter]()) {
          case (acc, publicNodeMapping) =>
            val publicMappings = findAllNodeMappings(publicNodeMapping.mappedNode().value()).map(_.id).toSet
            val declared = model.declares.collect {
              case elem: DialectDomainElement if publicMappings.contains(elem.definedBy.id) => elem
            }
            if (declared.nonEmpty) {
              findNodeMappingById(publicNodeMapping.mappedNode().value()) match {
                case (_, nodeMappable: NodeMappable) =>
                  acc ++ Seq(
                      DeclarationsGroupEmitter(
                          declared,
                          publicNodeMapping,
                          nodeMappable,
                          instance,
                          dialect,
                          ordering,
                          docs
                            .declarationsPath()
                            .option()
                            .getOrElse("/")
                            .split("/"),
                          referencesAliasIndex,
                          renderOptions = renderOptions
                      ))
              }
            } else acc
        }
      }
    }
    emitters.getOrElse(Nil)
  }
}

case class DialectNodeEmitter(node: DialectDomainElement,
                              nodeMappable: NodeMappable[_ <: NodeMappableModel],
                              references: Seq[BaseUnit],
                              dialect: Dialect,
                              ordering: SpecOrdering,
                              keyPropertyId: Option[String] = None,
                              discriminator: Option[(String, String)] = None,
                              emitDialect: Boolean = false,
                              topLevelEmitters: Seq[EntryEmitter] = Nil,
                              renderOptions: RenderOptions)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends PartEmitter
    with AmlEmittersHelper {

  override def emit(b: PartBuilder): Unit = {
    if (node.isLink)
      DialectDomainElementLinkEmitter(node, references).emit(b)
    else
      b.obj { b =>
        ordering.sorted(emitters).foreach(_.emit(b))
      }
  }

  def emitters: Seq[EntryEmitter] = {
    var emitters: Seq[EntryEmitter] = topLevelEmitters
    if (emitDialect) emitters ++= Seq(MapEntryEmitter("$dialect", nodeMappable.id))
    emitters ++= emitDiscriminator()
    emitters ++= emitId()
    emitters ++= emitCustomBase()
    emitters ++= fieldAndExtensionEmitters
    emitters
  }

  private def fieldAndExtensionEmitters: Seq[EntryEmitter] = {
    val fieldEmitter = NodeFieldEmitters(node,
                                         nodeMappable,
                                         references,
                                         dialect,
                                         ordering,
                                         keyPropertyId,
                                         discriminator,
                                         emitDialect,
                                         topLevelEmitters,
                                         renderOptions)
    uniqueFields(node.meta).flatMap { field =>
      field match {
        case DomainElementModel.CustomDomainProperties => CustomDomainPropertiesEmitter(node)
        case field                                     => fieldEmitter.emitField(field)
      }
    }.toSeq
  }

  private def uniqueFields(meta: DialectDomainElementModel): Iterable[Field] = {
    val allFields = meta.fields :+ DomainElementModel.CustomDomainProperties
    var acc       = Map[String, Field]()
    allFields.foreach { f =>
      acc.get(f.value.iri()) match {
        case Some(_) => // ignore
        case _ =>
          acc = acc.updated(f.value.iri(), f)
      }
    }
    acc.values
  }

  private def emitCustomBase(): Seq[EntryEmitter] = {
    customBaseOf(node)
      .filter(_.value != "true")
      .map(base => Seq(MapEntryEmitter("$base", base.value)))
      .getOrElse(Nil)
  }

  private def customBaseOf(node: DialectDomainElement) = node.annotations.find(classOf[CustomBase])

  private def emitId(): Seq[EntryEmitter] = {
    if (hasCustomId(node) || renderOptions.isEmitNodeIds) {
      val baseId = customIdOf(node) match {
        case Some(customId) if customId.value != "true" => customId.value
        case _                                          => node.id
      }
      val customId = if (baseId.contains(dialect.location().getOrElse(""))) {
        baseId.replace(dialect.id, "")
      } else {
        baseId
      }
      Seq(MapEntryEmitter("$id", customId))
    } else Nil
  }

  private def customIdOf(node: DialectDomainElement) = node.annotations.find(classOf[CustomId])

  private def hasCustomId(node: DialectDomainElement) = customIdOf(node).isDefined

  private def emitDiscriminator(): Seq[EntryEmitter] = {
    discriminator.map { case (name, value) => Seq(MapEntryEmitter(name, value)) }.getOrElse(Seq.empty)
  }

  override def position(): Position =
    node.annotations
      .find(classOf[LexicalInformation])
      .map(_.range.start)
      .getOrElse(ZERO)
}
