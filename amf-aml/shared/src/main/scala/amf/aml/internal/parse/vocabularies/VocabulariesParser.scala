package amf.aml.internal.parse.vocabularies

import amf.core.client.scala.model.DataType
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{AmfArray, AmfScalar}
import amf.core.client.scala.parse.document.SyamlParsedDocument
import amf.core.internal.parser.{Root, YMapOps}
import amf.core.internal.parser.domain.{Annotations, BaseSpecParser, DefaultArrayNode, ValueNode}
import amf.aml.internal.metamodel.document.VocabularyModel
import amf.aml.internal.metamodel.domain.{ClassTermModel, ObjectPropertyTermModel}
import amf.aml.client.scala.model.document.Vocabulary
import amf.aml.client.scala.model.domain._
import amf.aml.internal.parse.common.{DeclarationKey, DeclarationKeyCollector}
import org.yaml.model._
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.annotations.SourceSpec
import amf.core.internal.remote.Spec
import amf.core.internal.remote.Spec.AML

class VocabulariesParser(root: Root)(implicit override val ctx: VocabularyContext)
    extends BaseSpecParser
    with DeclarationKeyCollector {

  val map: YMap              = root.parsed.asInstanceOf[SyamlParsedDocument].document.as[YMap]
  val vocabulary: Vocabulary = Vocabulary(Annotations(map)).withLocation(root.location).withId(root.location)

  def parseDocument(): BaseUnit = {
    map.key("vocabulary") match {
      case Some(entry) => {
        val value = ValueNode(entry.value)
        vocabulary.set(VocabularyModel.Name, value.string(), Annotations(entry))
      }
      case None => ctx.missingVocabularyTermWarning(vocabulary.id, map)
    }

    map.key("base") match {
      case Some(entry) => {
        val value = ValueNode(entry.value)
        vocabulary.set(VocabularyModel.Base, value.string(), Annotations(entry))
      }
      case None => ctx.missingBaseTermViolation(vocabulary.id, map)
    }

    map.key("usage", entry => {
      val value = ValueNode(entry.value)
      vocabulary.set(VocabularyModel.Usage, value.string(), Annotations(entry))
    })

    // closed node validation
    ctx.closedNode("vocabulary", vocabulary.id, map)

    val references = VocabulariesReferencesParser(map, root.references).parse(vocabulary.base.value())

    if (ctx.declarations.externals.nonEmpty)
      vocabulary.withExternals(ctx.declarations.externals.values.toSeq)

    parseClassTerms(map)
    parsePropertyTerms(map)

    val declarables = ctx.terms()
    val imported = ctx.imported map {
      case (alias, library) =>
        VocabularyReference()
          .withAlias(alias)
          .withReference(library.id)
          .withBase(library.base.value())
          .adopted(vocabulary.id)
    }
    if (imported.nonEmpty)
      vocabulary.withImports(imported.toSeq)
    addDeclarationsToModel(vocabulary, declarables)
    if (references.nonEmpty) vocabulary.withReferences(references.baseUnitReferences())
    // we raise exceptions for missing terms
    ctx.pendingLocal.foreach {
      case (term, alias, location, isProperty) =>
        if (isProperty) {
          ctx.missingPropertyTermWarning(term, vocabulary.id, location)
        } else {
          ctx.missingClassTermWarning(term, vocabulary.id, location)
        }
    }

    vocabulary.annotations += SourceSpec(AML)
    vocabulary
  }

  def parseClassTerms(map: YMap): Unit = {
    map.key(
        "classTerms",
        entry => {
          addDeclarationKey(DeclarationKey(entry))
          val classDeclarations = entry.value.as[YMap]
          classDeclarations.entries.foreach { classTermDeclaration =>
            parseClassTerm(classTermDeclaration)
          }
        }
    )
  }

  def parseClassTerm(classTermDeclaration: YMapEntry): Unit = {
    val classTerm      = ClassTerm(Annotations(classTermDeclaration))
    val classTermAlias = classTermDeclaration.key.as[YScalar].text
    classTerm.withName(classTermAlias)

    ctx.resolveClassTermAlias(vocabulary.base.value(), classTermAlias, classTermDeclaration.key, strictLocal = false) match {
      case None     => ctx.missingClassTermWarning(classTermAlias, vocabulary.id, classTermDeclaration.key)
      case Some(id) => classTerm.id = id
    }

    classTermDeclaration.value.tagType match {
      case YType.Null => // just declaration
      case _ =>
        val classTermMap = classTermDeclaration.value.as[YMap]
        ctx.closedNode("classTerm", classTerm.id, classTermMap)

        classTermMap.key("displayName", entry => {
          val value = ValueNode(entry.value)
          classTerm.set(ClassTermModel.DisplayName, value.string())
        })

        classTermMap.key("description", entry => {
          val value = ValueNode(entry.value)
          classTerm.set(ClassTermModel.Description, value.string())
        })

        classTermMap.key(
            "properties",
            entry => {
              val refs: Seq[String] = entry.value.tagType match {
                case YType.Str => Seq(ValueNode(entry.value).string().toString)
                case YType.Seq =>
                  DefaultArrayNode(entry.value).nodes._1
                    .map(_.value.toString) // ArrayNode(entry.value).strings().scalars.map(_.toString)
                case YType.Null => Seq.empty
              }

              val properties: Seq[AmfScalar] = refs
                .map { term: String =>
                  ctx.resolvePropertyTermAlias(vocabulary.base.value(), term, entry.value, strictLocal = true) match {
                    case Some(v) => Some(AmfScalar(v))
                    case None =>
                      ctx.missingPropertyTermWarning(term, classTerm.id, entry.value)
                      None
                  }
                }
                .filter(_.nonEmpty)
                .map(_.get)

              if (properties.nonEmpty)
                classTerm.set(ClassTermModel.Properties,
                              AmfArray(properties, Annotations(entry.value)),
                              Annotations(entry))
            }
        )

        classTermMap.key(
            "extends",
            entry => {
              val refs: Seq[String] = entry.value.tagType match {
                case YType.Str => Seq(ValueNode(entry.value).string().toString)
                case YType.Seq =>
                  // ArrayNode(entry.value).strings().scalars.map(_.toString)
                  DefaultArrayNode(node = entry.value).nodes._1.map(_.value.toString)
                case YType.Null => Seq.empty
              }

              val superClasses: Seq[String] = refs
                .map { term: String =>
                  ctx.resolveClassTermAlias(vocabulary.base.value(), term, entry.value, strictLocal = true) match {
                    case Some(v) => Some(v)
                    case None =>
                      ctx.missingClassTermWarning(term, classTerm.id, entry.value)
                      None
                  }
                }
                .filter(_.nonEmpty)
                .map(_.get)

              classTerm.set(ClassTermModel.SubClassOf, superClasses)
            }
        )
    }

    ctx.register(classTermAlias, classTerm)
  }

  def parsePropertyTerms(map: YMap): Unit = {
    map.key(
        "propertyTerms",
        entry => {
          addDeclarationKey(DeclarationKey(entry))
          val classDeclarations = entry.value.as[YMap]
          classDeclarations.entries.foreach { propertyTermDeclaration =>
            parsePropertyTerm(propertyTermDeclaration)
          }
        }
    )
  }

  def parsePropertyTerm(propertyTermDeclaration: YMapEntry): Unit = {
    val propertyTerm: PropertyTerm = propertyTermDeclaration.value.tagType match {
      case YType.Null => DatatypePropertyTerm(Annotations(propertyTermDeclaration))
      case _ =>
        propertyTermDeclaration.value.as[YMap].key("range") match {
          case None => DatatypePropertyTerm(Annotations(propertyTermDeclaration))
          case Some(value) =>
            value.value.as[YScalar].text match {
              case "string" | "integer" | "float" | "double" | "long" | "boolean" | "uri" | "any" | "time" | "date" |
                  "dateTime" =>
                DatatypePropertyTerm(Annotations(propertyTermDeclaration))
              case _ => ObjectPropertyTerm(Annotations(propertyTermDeclaration))
            }
        }
    }

    val propertyTermAlias = propertyTermDeclaration.key.as[YScalar].text
    propertyTerm.withName(propertyTermAlias)

    ctx.resolvePropertyTermAlias(vocabulary.base.value(),
                                 propertyTermAlias,
                                 propertyTermDeclaration.key,
                                 strictLocal = false) match {
      case None     => ctx.missingPropertyTermWarning(propertyTermAlias, vocabulary.id, propertyTermDeclaration.key)
      case Some(id) => propertyTerm.id = id
    }

    propertyTermDeclaration.value.tagType match {
      case YType.Null => // ignore
      case _ =>
        val propertyTermMap = propertyTermDeclaration.value.as[YMap]
        ctx.closedNode("propertyTerm", propertyTerm.id, propertyTermMap)

        propertyTermMap.key("displayName", entry => {
          val value = ValueNode(entry.value)
          propertyTerm.set(ClassTermModel.DisplayName, value.string())
        })

        propertyTermMap.key("description", entry => {
          val value = ValueNode(entry.value)
          propertyTerm.set(ClassTermModel.Description, value.string())
        })

        propertyTermMap.key(
            "range",
            entry => {
              val text = entry.value.as[YScalar].text
              val rangeId = text match {
                case "guid" =>
                  Some((Namespace.Shapes + "guid").iri())
                case "any" | "uri" | "string" | "integer" | "float" | "double" | "long" | "boolean" | "time" | "date" |
                    "dateTime" =>
                  Some(DataType(text))
                case classAlias =>
                  ctx.resolveClassTermAlias(vocabulary.base.value(), classAlias, entry.value, strictLocal = true) match {
                    case Some(classTermId) => Some(classTermId)
                    case None =>
                      ctx.missingClassTermWarning(classAlias, propertyTerm.id, entry.value)
                      None
                  }
              }

              rangeId match {
                case Some(id: String) => propertyTerm.withRange(id)
                case None             => // ignore
              }
            }
        )

        propertyTermMap.key(
            "extends",
            entry => {
              val refs: Seq[String] = entry.value.tagType match {
                case YType.Str => Seq(ValueNode(entry.value).string().toString)
                case YType.Seq =>
                  DefaultArrayNode(entry.value).nodes._1.map(_.as[YScalar].text)
                // ArrayNode(entry.value).strings().scalars.map(_.toString)
                case YType.Null => Seq.empty
              }

              val superClasses: Seq[String] = refs
                .map { term: String =>
                  ctx.resolvePropertyTermAlias(vocabulary.base.value(), term, entry.value, strictLocal = true) match {
                    case Some(v) => Some(v)
                    case None =>
                      ctx.missingPropertyTermWarning(term, propertyTerm.id, entry.value)
                      None
                  }
                }
                .filter(_.nonEmpty)
                .map(_.get)

              propertyTerm.set(ObjectPropertyTermModel.SubPropertyOf, superClasses)
            }
        )
    }

    ctx.register(propertyTermAlias, propertyTerm)
  }
}
