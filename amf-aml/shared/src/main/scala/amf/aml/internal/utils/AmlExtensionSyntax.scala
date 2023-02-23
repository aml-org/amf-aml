package amf.aml.internal.utils

import amf.aml.client.scala.model.document.{Dialect, DialectFragment, DialectLibrary}
import amf.aml.client.scala.model.domain._
import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel}
import amf.core.client.scala.model.domain.Linkable
import amf.core.internal.annotations.Aliases
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.aml.internal.transform.domain.NodeMappingResolver

import scala.collection.mutable

// Scala syntax extensions
package object AmlExtensionSyntax {
  type NodeMappable = NodeMappable.AnyNodeMappable

  implicit class RichExternalsSeq(val externals: Seq[External]) extends AnyVal {

    /** Fixes collisions when nested externals use the same alias
      *
      * @return
      *   external sequence with handled collisions
      */
    def fixAliasCollisions: Seq[External] = {
      val aliasIndex: mutable.Map[Aliases.Alias, Aliases.FullUrl] = mutable.Map.empty

      externals.flatMap { external =>
        aliasIndex.get(external.alias.value()) match {
          case Some(uri) if uri == external.base.value() =>
            None // Already added, skip
          case Some(_) => // Handle collision
            val alias   = external.alias.value()
            val uriHash = external.base.value().hashCode
            external
              .withAlias(
                s"$alias-$uriHash"
              ) // Appending the uri hash avoids enumeration problems & guarantees uniqueness
            Some(external)
          case None =>
            aliasIndex.put(external.alias.value(), external.base.value())
            Some(external)
        }
      }
    }
  }

  implicit class RichBaseUnit(val baseUnit: BaseUnit) extends AnyVal {
    def recursivelyFindExternals(model: BaseUnit = this.baseUnit): Seq[External] = {
      val externals = model match {
        case lib: DialectLibrary       => lib.externals
        case dialect: Dialect          => dialect.externals
        case fragment: DialectFragment => fragment.externals
        case _                         => Nil
      }

      val nestedExternals = model.references.flatMap(recursivelyFindExternals)

      externals ++ nestedExternals
    }

    def recursivelyFindDeclarations(
        model: BaseUnit = this.baseUnit,
        acc: Map[String, NodeMappable] = Map()
    ): Map[String, NodeMappable] = {
      val updateDeclarations = model match {
        case lib: DeclaresModel =>
          lib.declares.collect { case nodeMapping: NodeMappable => nodeMapping }.foldLeft(acc) { case (acc, mapping) =>
            acc.updated(mapping.id, mapping)
          }
        case _ => acc
      }

      model.references.collect { case lib: DeclaresModel => lib }.foldLeft(updateDeclarations) { case (acc, lib) =>
        recursivelyFindDeclarations(lib, acc)
      }
    }
  }

  implicit class RichNodeMappable(val nodeMappable: NodeMappable) extends AnyVal {
    def copyMapping: NodeMappable with Linkable with MergeableMapping = {
      val fields = Fields()
      nodeMappable.fields.fields().foreach { entry =>
        fields.setWithoutId(entry.field, entry.value.value, entry.value.annotations)
      }
      nodeMappable match {
        case _: NodeMapping =>
          NodeMapping(fields, Annotations())
        case _: UnionNodeMapping =>
          new UnionNodeMapping(fields, Annotations())
      }
    }
  }

  implicit class RichNodeMapping(val nodeMapping: NodeMapping) {
    lazy val resolver: NodeMappingResolver = new NodeMappingResolver(nodeMapping)
  }

}
