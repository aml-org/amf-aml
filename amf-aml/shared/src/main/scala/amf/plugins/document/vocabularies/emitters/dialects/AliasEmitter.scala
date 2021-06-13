package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.internal.render.BaseEmitters.MapEntryEmitter
import amf.core.internal.render.emitters.EntryEmitter
import amf.core.internal.metamodel.Field
import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.domain.DomainElement
import org.yaml.model.YType

trait AliasEmitter extends AliasesConsumer with PosExtractor {

  protected val element: DomainElement

  protected def emitAlias(key: String, field: StrField, metaField: Field, yType: YType): Option[EntryEmitter] = {
    aliasFor(field.value()).map { targetAlias =>
      val pos = fieldPos(element, metaField)
      MapEntryEmitter(key, targetAlias, yType, pos)
    }
  }
}
