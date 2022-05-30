package amf.aml.internal.render.emitters.dialects

import amf.core.client.common.position.Position
import amf.core.client.common.position.Position.ZERO
import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel}
import amf.core.internal.render.BaseEmitters.traverse
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import amf.aml.internal.annotations.AliasesLocation
import org.yaml.model.YDocument.EntryBuilder

case class ReferencesEmitter(baseUnit: BaseUnit, ordering: SpecOrdering, aliases: Map[String, (String, String)])
    extends EntryEmitter {
  val modules: Seq[BaseUnit with DeclaresModel] = baseUnit.references.collect({ case m: DeclaresModel => m })
  override def emit(b: EntryBuilder): Unit = {
    if (modules.nonEmpty) {
      b.entry(
          "uses",
          _.obj { b =>
            traverse(ordering.sorted(modules.map(r => ReferenceEmitter(r, ordering, aliases))), b)
          }
      )
    }
  }

  override def position(): Position =
    baseUnit.annotations.find(classOf[AliasesLocation]).map(annot => Position((annot.position, 0))).getOrElse(ZERO)
}
