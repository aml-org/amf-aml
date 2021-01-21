package amf.utils.internal

import amf.core.annotations.Aliases
import amf.core.model.document.{BaseUnit, DeclaresModel}
import amf.core.model.domain.Linkable
import amf.core.parser.{Annotations, Fields}
import amf.plugins.document.vocabularies.metamodel.domain.NodeMappingModel
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectFragment, DialectLibrary}
import amf.plugins.document.vocabularies.model.domain.{
  External,
  MergeableMapping,
  NodeMappable,
  NodeMapping,
  PropertyMapping,
  UnionNodeMapping
}

import scala.collection.mutable

// Scala syntax extensions
package object AmlExtensionSyntax {

  implicit class RichExternalsSeq(val externals: Seq[External]) extends AnyVal {

    /**
      * Fixes collisions when nested externals use the same alias
      *
      * @return external sequence with handled collisions
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
              .withAlias(s"$alias-$uriHash") // Appending the uri hash avoids enumeration problems & guarantees uniqueness
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

    def recursivelyFindDeclarations(model: BaseUnit = this.baseUnit,
                                    acc: Map[String, NodeMappable] = Map()): Map[String, NodeMappable] = {
      val updateDeclarations = model match {
        case lib: DeclaresModel =>
          lib.declares.collect { case nodeMapping: NodeMappable => nodeMapping }.foldLeft(acc) {
            case (acc, mapping) =>
              acc.updated(mapping.id, mapping)
          }
        case _ => acc
      }

      model.references.collect { case lib: DeclaresModel => lib }.foldLeft(updateDeclarations) {
        case (acc, lib) =>
          recursivelyFindDeclarations(lib, acc)
      }
    }
  }

  implicit class RichNodeMappable(val nodeMappable: NodeMappable) extends AnyVal {
    def cloneMapping: NodeMappable with Linkable with MergeableMapping = {
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

  implicit class RichNodeMapping(val nodeMapping: NodeMapping) extends AnyVal {
    def resolveExtension: NodeMapping = {
      nodeMapping.extend match {
        case (parent: NodeMapping) :: _ =>
          val superMerged = parent.resolveExtension
          superMerged.idTemplate.option() match {
            case Some(idTemplate) =>
              nodeMapping.idTemplate.option() match {
                case None => nodeMapping.withIdTemplate(idTemplate)
                case _    => // ignore
              }
            case _ => // ignore
          }

          val merged = nodeMapping.mergeWith(superMerged)
          // we store the extended reference and remove the extends property
          merged.withResolvedExtends(Seq(parent.id))
          merged.fields.removeField(NodeMappingModel.Extends)
          // return the final node
          merged
        case _ =>
          // Ignore
      }
      nodeMapping
    }

    def mergeWith(other: NodeMapping): NodeMapping = {
      val acc = mutable.Map[String, PropertyMapping]()
      nodeMapping.propertiesMapping().foreach { prop =>
        acc += (prop.name().value() -> prop)
      }

      other.propertiesMapping().foreach { property =>
        acc.get(property.name().value()) match {
          case Some(_) => // Ignore
          case None =>
            acc += (property.name().value() -> PropertyMapping(property.fields.copy(), property.annotations.copy())
              .withId(property.id))
        }
      }

      nodeMapping.withPropertiesMapping(acc.values.toList)
    }
  }

}
