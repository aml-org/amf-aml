package amf.aml.internal.render.emitters.dialects

import amf.core.internal.render.BaseEmitters.{ArrayEmitter, MapEntryEmitter}
import amf.core.internal.render.emitters.EntryEmitter
import amf.core.internal.metamodel.Field
import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.domain.{AmfArray, AmfScalar, DomainElement}
import amf.core.internal.parser.domain.{FieldEntry, Value}
import amf.core.internal.render.SpecOrdering
import org.yaml.model.{YMapEntry, YType}

trait AliasEmitter extends AliasesConsumer with PosExtractor {

  protected val element: DomainElement

  protected def emitAlias(key: String, field: StrField, metaField: Field, yType: YType): Option[EntryEmitter] = {
    aliasFor(field.value()).map { targetAlias =>
      val pos = fieldPos(element, metaField)
      MapEntryEmitter(key, targetAlias, yType, pos)
    }
  }

  protected def emitAliasSet(key: String, entry: FieldEntry, ordering: SpecOrdering, yType: YType): EntryEmitter = {
    val scalars = entry.array.scalars.map { scalar =>
      aliasFor(scalar.value.toString).map(alias => scalar.copy(value = alias)).getOrElse(scalar)
    }
    val nextEntry = entry.copy(value = new Value(entry.array.copy(values = scalars), entry.value.annotations))
    ArrayEmitter(key, nextEntry, ordering = ordering, valuesTag = yType)
  }
}
