package amf.aml.internal.parse.vocabularies

import amf.aml.client.scala.model.document.Vocabulary
import amf.aml.client.scala.model.domain.ClassTerm
import amf.aml.internal.metamodel.domain.ClassTermModel
import amf.aml.internal.parse.dialects.DialectAstOps.DialectYMapOps
import amf.core.client.scala.model.domain.{AmfArray, AmfScalar}
import amf.core.internal.parser.domain.{Annotations, ValueNode}
import org.yaml.model.{YMap, YMapEntry, YScalar, YType}

object ClassTermParser extends SingleOrMultipleItems with IriReferenceParsing {
  def parse(entry: YMapEntry, vocabulary: Vocabulary)(implicit ctx: VocabularyContext): ClassTerm = {
    val classTerm      = ClassTerm(Annotations(entry))
    val classTermAlias = entry.key.as[YScalar].text
    classTerm.withName(classTermAlias)

    ctx.resolveClassTermAlias(vocabulary.base.value(), classTermAlias, entry.key, strictLocal = false) match {
      case None     => ctx.missingClassTermWarning(classTermAlias, vocabulary.id, entry.key)
      case Some(id) => classTerm.id = id
    }

    entry.value.tagType match {
      case YType.Null =>
      case _ =>
        val map = entry.value.as[YMap]
        ctx.closedNode("classTerm", classTerm.id, map)
        parseDisplayName(map, classTerm)
        parseDescription(map, classTerm)
        parseProperties(map, classTerm, vocabulary)
        parseExtends(map, classTerm, vocabulary) // just declaration
    }

    ctx.register(classTermAlias, classTerm)
    classTerm
  }

  private def parseExtends(classTermMap: YMap, classTerm: ClassTerm, vocabulary: Vocabulary)(
      implicit ctx: VocabularyContext) = {
    classTermMap.key(
        "extends",
        entry => {
          val terms = singleOrMultipleItemsAsString(entry)
          val superClasses = parseIriAlias(
              terms,
              term => ctx.resolveClassTermAlias(vocabulary.base.value(), term, entry.value, strictLocal = true),
              term => ctx.missingClassTermWarning(term, classTerm.id, entry.value)
          )

          classTerm.set(ClassTermModel.SubClassOf,
                        AmfArray(superClasses, Annotations(entry.value)),
                        Annotations(entry))
        }
    )
  }

  private def parseProperties(classTermMap: YMap, classTerm: ClassTerm, vocabulary: Vocabulary)(
      implicit ctx: VocabularyContext) = {
    classTermMap.key(
        "properties",
        entry => {
          val terms = singleOrMultipleItemsAsString(entry)
          val properties = parseIriAlias(
              terms,
              term => ctx.resolvePropertyTermAlias(vocabulary.base.value(), term, entry.value, strictLocal = true),
              term => ctx.missingPropertyTermWarning(term, classTerm.id, entry.value)
          )

          if (properties.nonEmpty)
            classTerm.set(ClassTermModel.Properties,
                          AmfArray(properties, Annotations(entry.value)),
                          Annotations(entry))
        }
    )
  }

  private def parseDescription(classTermMap: YMap, classTerm: ClassTerm) = {
    classTermMap.key("description", entry => {
      val value = ValueNode(entry.value)
      classTerm.set(ClassTermModel.Description, value.string())
    })
  }

  private def parseDisplayName(classTermMap: YMap, classTerm: ClassTerm) = {
    classTermMap.key("displayName", entry => {
      val value = ValueNode(entry.value)
      classTerm.set(ClassTermModel.DisplayName, value.string())
    })
  }
}
