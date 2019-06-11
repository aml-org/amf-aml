package amf.plugins.document.vocabularies.resolution.stages

import amf.core.annotations.Aliases
import amf.core.model.document.{BaseUnit, DeclaresModel}
import amf.core.parser.{Annotations, ErrorHandler, Fields}
import amf.core.resolution.stages.ResolutionStage
import amf.plugins.document.vocabularies.metamodel.domain.NodeMappingModel
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectFragment, DialectLibrary, Vocabulary}
import amf.plugins.document.vocabularies.model.domain.{External, NodeMappable, NodeMapping, UnionNodeMapping}

import scala.collection.mutable

class DialectReferencesResolutionStage()(override implicit val errorHandler: ErrorHandler) extends ResolutionStage() {

  def findDeclarations(model: BaseUnit, acc: Map[String, NodeMappable] = Map()): Map[String, NodeMappable] = {
    val updateDeclarations = model match {
      case lib: DeclaresModel => {
        lib.declares.collect { case nodeMapping: NodeMappable => nodeMapping }.foldLeft(acc) {
          case (acc, mapping) =>
            acc.updated(mapping.id, mapping)
        }
      }
      case _ => acc
    }

    model.references.collect { case lib: DeclaresModel => lib }.foldLeft(updateDeclarations) {
      case (acc, lib) =>
        findDeclarations(lib, acc)
    }
  }

  def findExternals(model: BaseUnit, acc: Set[String] = Set()): Set[String] = {
    val updateDeclarations = model match {
      case lib: DialectLibrary       => acc ++ lib.externals.map(_.base.value())
      case dialect: Dialect          => acc ++ dialect.externals.map(_.base.value())
      case fragment: DialectFragment => acc ++ fragment.externals.map(_.base.value())
      case _                         => acc
    }

    model.references.foldLeft(updateDeclarations) {
      case (acc, lib) =>
        findExternals(lib, acc)
    }
  }

  def findVocabularies(model: BaseUnit, acc: Set[Vocabulary] = Set()): Set[Vocabulary] = {
    val updateDeclarations = model.references.foldLeft(acc) {
      case (acc, ref) =>
        acc ++ ref.references.collect { case vocab: Vocabulary => vocab }
    }

    model.references.collect { case lib: DialectLibrary => lib }.foldLeft(updateDeclarations) {
      case (acc, lib) =>
        findVocabularies(lib, acc)
    }
  }

  def dereference(nodeMappable: NodeMappable, finalDeclarations: mutable.Map[String, NodeMappable]): NodeMappable = {
    finalDeclarations.get(nodeMappable.id) match {
      case Some(mappable) => mappable
      case _              =>
        nodeMappable match {
          // Resolving links in node mappings declarations
          case mapping: NodeMapping =>
            val finalNode = if (mapping.isLink) {
              val target = dereference(mapping.linkTarget.get.asInstanceOf[NodeMapping], finalDeclarations).asInstanceOf[NodeMapping]
              target.withName(mapping.name.value()).withId(target.id)
            } else {
              mapping
            }

            val extended = finalNode.extend.map {
              case superNode: NodeMapping =>
                val target = dereference(superNode, finalDeclarations).asInstanceOf[NodeMapping]
                Some(target)
              case _                      =>
                None

            }
            finalNode.withExtends(extended.collect { case Some(n) => n})
            finalDeclarations += (finalNode.id -> finalNode)
            finalNode

          // we ignore them in unions
          case union: UnionNodeMapping => {
            finalDeclarations += (union.id -> union)
            union
          }
        }
    }
  }

  def cloneNodeMapping(target: NodeMappable) = {
    val fields = Fields()
    target.fields.fields().foreach { entry =>
      fields.setWithoutId(entry.field, entry.value.value, entry.value.annotations)
    }
    target match {
      case _: NodeMapping =>
        NodeMapping(fields, Annotations())
      case _: UnionNodeMapping =>
         new UnionNodeMapping(fields, Annotations())
    }
  }

  def genName(baseName: String, allDeclarations: Map[String, NodeMappable]): String = {
    var c = 1
    var acc = baseName
    while (allDeclarations.contains(acc)) {
      c += 1
      acc = s"$baseName$c"
    }
    acc
  }

  def dereferencePendingDeclarations(pending: Seq[NodeMappable], acc: mutable.Map[String, NodeMappable], allDeclarations: Map[String, NodeMappable]): Unit = {
    if (pending.nonEmpty) {
      val nextPending = pending.head
      // has this been already dereferenced with some alias
      acc.get(nextPending.id) match {
        case Some(_) => // ignore already added, ignore
          dereferencePendingDeclarations(pending.tail, acc, allDeclarations)
        case None    =>
          val effectiveNextPending = if (nextPending.isLink) {
            // if this is a link, we clone
            cloneNodeMapping(nextPending.effectiveLinkTarget().asInstanceOf[NodeMappable]).withName(nextPending.name.value()).withId(nextPending.id)
          } else {
            // otherwise we just introduce the node mapping
            nextPending
          }
          val newPendingRange = effectiveNextPending match {
            case nodeMapping: NodeMapping =>
              // we add all object ranges to the list of pendings
              pending.tail ++ nodeMapping.propertiesMapping().flatMap(_.objectRange()).map(r => allDeclarations.get(r.value())).collect { case Some(x) => x }
            case union: UnionNodeMapping =>
              // we add all union ranges to the list of pendings
              pending.tail ++ union.objectRange().map(r => allDeclarations.get(r.value())).collect { case Some(x) => x }
          }

          val newPending = effectiveNextPending.extend.headOption match {
            case Some(n: NodeMappable) if n.linkTarget.isDefined => newPendingRange ++ Seq(n.linkTarget.get.asInstanceOf[NodeMappable])
            case _                                               => newPendingRange
          }

          if (effectiveNextPending.name.value().contains(".")) { // this might come from a library TODO: check collisions in names
            effectiveNextPending.withName(genName(effectiveNextPending.name.value().split(".").last, allDeclarations))
          }

          acc += (effectiveNextPending.id -> effectiveNextPending)

          dereferencePendingDeclarations(newPending, acc, allDeclarations)

      }

    }
  }

  // Set the final extend references to the final list of node mappings
  def linkExtendedNodes(acc: mutable.Map[String, NodeMappable]): Unit = {
    acc.values.foreach { nodeMappable =>
      nodeMappable.extend.headOption match {
        case Some(extended: NodeMappable) if extended.linkTarget.isDefined && acc.contains(extended.linkTarget.get.id) =>
          val found = acc(extended.linkTarget.get.id)
          nodeMappable.setArrayWithoutId(NodeMappingModel.Extends, Seq(found))
        case _                        =>
          // ignore
      }
    }
  }

  override def resolve[T <: BaseUnit](model: T): T = {

    var allDeclarations = findDeclarations(model)
    var allExternals    = findExternals(model)
    var allVocabularies = findVocabularies(model)

    val finalDeclarationsMap = mutable.Map[String, NodeMappable]()
    val unitDeclarations = model.asInstanceOf[DeclaresModel].declares.filter(_.isInstanceOf[NodeMappable]).asInstanceOf[Seq[NodeMappable]]

    dereferencePendingDeclarations(pending = unitDeclarations, acc = finalDeclarationsMap, allDeclarations = allDeclarations)
    linkExtendedNodes(finalDeclarationsMap)

    val finalDeclarations = finalDeclarationsMap.values.toSeq

    val vocabulariesAliases =
      allVocabularies.zipWithIndex.foldLeft(Map[Aliases.Alias, (Aliases.FullUrl, Aliases.RelativeUrl)]()) {
        case (acc, (vocab, i)) =>
          acc.updated(s"vocab$i", (vocab.id, vocab.id))
      }

    val finalExternals: Seq[External] = allExternals.toSeq.zipWithIndex.map {
      case (external: String, i) =>
        External()
          .withBase(external)
          .withAlias(s"external$i")
          .withId(model.location().getOrElse(model.id) + s"#/external/external$i")
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

    resolved.annotations += Aliases(vocabulariesAliases.toSet)

    resolved.asInstanceOf[T]
  }

}
