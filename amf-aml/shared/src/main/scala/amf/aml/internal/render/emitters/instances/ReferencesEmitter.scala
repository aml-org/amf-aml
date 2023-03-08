package amf.aml.internal.render.emitters.instances

import amf.aml.internal.annotations.AliasesLocation
import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel}
import amf.core.internal.render.BaseEmitters.traverse
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import org.mulesoft.common.client.lexical.Position
import org.mulesoft.common.client.lexical.Position.ZERO
import org.yaml.model.YDocument.EntryBuilder

case class ReferencesEmitter(baseUnit: BaseUnit, ordering: SpecOrdering, aliases: Map[String, (String, String)])
    extends EntryEmitter {
  override def emit(b: EntryBuilder): Unit = {
    val modules = baseUnit.references.collect({ case m: DeclaresModel => m })
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
    baseUnit.annotations
      .find(classOf[AliasesLocation])
      .map(annot => Position((annot.position, 0)))
      .getOrElse(ZERO)
}
