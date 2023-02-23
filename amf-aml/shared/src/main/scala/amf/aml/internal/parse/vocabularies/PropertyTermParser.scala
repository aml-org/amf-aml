package amf.aml.internal.parse.vocabularies

import amf.aml.client.scala.model.document.Vocabulary
import amf.aml.client.scala.model.domain.{ClassTerm, DatatypePropertyTerm, ObjectPropertyTerm, PropertyTerm}
import amf.aml.internal.metamodel.domain.{ClassTermModel, DatatypePropertyTermModel, ObjectPropertyTermModel}
import amf.aml.internal.parse.dialects.DialectAstOps.DialectYMapOps
import amf.core.client.scala.model.DataType
import amf.core.client.scala.model.domain.{AmfArray, AmfScalar}
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.parser.domain.{Annotations, DefaultArrayNode, ValueNode}
import org.yaml.model.{YMap, YMapEntry, YScalar, YType}

case class PropertyTermParser()(implicit val ctx: VocabularyContext)
    extends SingleOrMultipleItems
    with IriReferenceParsing {

  def parse(entry: YMapEntry, vocabulary: Vocabulary): PropertyTerm = {
    val propertyTerm      = createPropertyTerm(entry)
    val propertyTermAlias = entry.key.as[YScalar].text
    propertyTerm.withName(propertyTermAlias)

    ctx.resolvePropertyTermAlias(vocabulary.base.value(), propertyTermAlias, entry.key, strictLocal = false) match {
      case None     => ctx.missingPropertyTermWarning(propertyTermAlias, vocabulary.id, entry.key)
      case Some(id) => propertyTerm.id = id
    }

    entry.value.tagType match {
      case YType.Null => // ignore
      case _ =>
        val map = entry.value.as[YMap]
        ctx.closedNode("propertyTerm", propertyTerm.id, map)
        parseDisplayName(map, propertyTerm)
        parseDescription(map, propertyTerm)
        parseRange(map, propertyTerm, vocabulary)
        parseExtends(map, propertyTerm, vocabulary)
    }

    ctx.register(propertyTermAlias, propertyTerm)
    propertyTerm
  }

  private def parseExtends(map: YMap, propertyTerm: PropertyTerm, vocabulary: Vocabulary)(implicit
      ctx: VocabularyContext
  ) = {
    map.key(
      "extends",
      entry => {
        val refs = singleOrMultipleItemsAsString(entry)
        val superClasses = parseIriAlias(
          refs,
          term => ctx.resolvePropertyTermAlias(vocabulary.base.value(), term, entry.value, strictLocal = true),
          term => ctx.missingPropertyTermWarning(term, propertyTerm.id, entry.value)
        )
        propertyTerm.set(
          ObjectPropertyTermModel.SubPropertyOf,
          AmfArray(superClasses, Annotations(entry.value)),
          Annotations(entry)
        )
      }
    )
  }

  private def parseRange(map: YMap, propertyTerm: PropertyTerm, vocabulary: Vocabulary)(implicit
      ctx: VocabularyContext
  ) = {
    map.key(
      "range",
      entry => {
        val text = entry.value.as[YScalar].text
        val rangeId = text match {
          case "guid" =>
            Some(AmfScalar((Namespace.Shapes + "guid").iri(), Annotations(entry.value)))
          case "any" | "uri" | "string" | "integer" | "float" | "double" | "long" | "boolean" | "time" | "date" |
              "dateTime" =>
            Some(AmfScalar(DataType(text), Annotations(entry.value)))
          case classAlias =>
            ctx.resolveClassTermAlias(vocabulary.base.value(), classAlias, entry.value, strictLocal = true) match {
              case Some(classTermId) => Some(AmfScalar(classTermId, Annotations.synthesized()))
              case None =>
                ctx.missingClassTermWarning(classAlias, propertyTerm.id, entry.value)
                None
            }
        }

        rangeId.foreach(scalar => propertyTerm.set(DatatypePropertyTermModel.Range, scalar, Annotations(entry)))
      }
    )
  }

  private def parseDescription(map: YMap, propertyTerm: PropertyTerm) = {
    map.key(
      "description",
      entry => {
        val value = ValueNode(entry.value)
        propertyTerm.set(ClassTermModel.Description, value.string())
      }
    )
  }

  private def parseDisplayName(map: YMap, propertyTerm: PropertyTerm) = {
    map.key(
      "displayName",
      entry => {
        val value = ValueNode(entry.value)
        propertyTerm.set(ClassTermModel.DisplayName, value.string())
      }
    )
  }

  private def createPropertyTerm(entry: YMapEntry) = {
    val propertyTerm: PropertyTerm = entry.value.tagType match {
      case YType.Null => DatatypePropertyTerm(Annotations(entry))
      case _ =>
        entry.value.as[YMap].key("range") match {
          case None => DatatypePropertyTerm(Annotations(entry))
          case Some(value) =>
            value.value.as[YScalar].text match {
              case "string" | "integer" | "float" | "double" | "long" | "boolean" | "uri" | "any" | "time" | "date" |
                  "dateTime" =>
                DatatypePropertyTerm(Annotations(entry))
              case _ => ObjectPropertyTerm(Annotations(entry))
            }
        }
    }
    propertyTerm
  }
}
