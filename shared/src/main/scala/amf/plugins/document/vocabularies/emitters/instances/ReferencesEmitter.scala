package amf.plugins.document.vocabularies.emitters.instances
import amf.core.emitter.BaseEmitters.traverse
import amf.core.emitter.{EntryEmitter, SpecOrdering}
import amf.core.model.document.{BaseUnit, DeclaresModel}
import amf.core.parser.Position
import amf.core.parser.Position.ZERO
import amf.plugins.document.vocabularies.annotations.AliasesLocation
import org.yaml.model.YDocument.EntryBuilder

case class ReferencesEmitter(baseUnit: BaseUnit, ordering: SpecOrdering, aliases: Map[String, (String, String)])
    extends EntryEmitter {
  override def emit(b: EntryBuilder): Unit = {
    val modules = baseUnit.references.collect({ case m: DeclaresModel => m })
    if (modules.nonEmpty) {

      b.entry("uses", _.obj { b =>
        traverse(ordering.sorted(modules.map(r => ReferenceEmitter(r, ordering, aliases))), b)
      })
    }
  }

  override def position(): Position =
    baseUnit.annotations
      .find(classOf[AliasesLocation])
      .map(annot => Position((annot.position, 0)))
      .getOrElse(ZERO)
}
