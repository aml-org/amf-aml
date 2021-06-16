package amf.validation.internal.emitters

import amf._
import amf.core.client.common.position.Position
import amf.core.client.common.validation.{Oas20Profile, ProfileName, Raml08Profile, Raml10Profile}
import amf.core.client.scala.model.DataType
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.plugins.document.graph.JsonLdKeywords
import amf.core.internal.render.BaseEmitters._
import amf.core.internal.render.emitters.PartEmitter
import amf.core.internal.validation.core.{FunctionConstraint, PropertyConstraint, ValidationSpecification}
import amf.core.internal.validation.model.{AlternatePath, PredicatePath, PropertyPath, SequencePath}
import org.yaml.model.YDocument._
import org.yaml.model.{YDocument, YType}
import org.yaml.render.JsonRender

import scala.collection.mutable.ListBuffer

/**
  * Generates a JSON-LD graph with the shapes for a set of validations
  * @param targetProfile which kind of messages should be generated
  */
class ShaclJsonLdShapeGraphEmitter(targetProfile: ProfileName) {

  private val jsValidatorEmitters: ListBuffer[PartEmitter]  = ListBuffer()
  private val jsConstraintEmitters: ListBuffer[PartEmitter] = ListBuffer()

  /**
    * Emit the JSON-LD for these validations
    * @param validations validations
    * @return JSON-LD graph with the validations
    */
  def emit(validations: Seq[ValidationSpecification]): String = JsonRender.render(buildDocument(validations))

  private def buildDocument(validations: Seq[ValidationSpecification]): YDocument = {
    YDocument {
      _.list { b =>
        validations.foreach(emitValidation(b, _))
        jsValidatorEmitters.foreach(_.emit(b))
        jsConstraintEmitters.foreach(_.emit(b))
      }
    }
  }

  private def emitValidation(b: PartBuilder, validation: ValidationSpecification): Unit = {
    val validationId = validation.id

    b.obj { p =>
      p.entry(JsonLdKeywords.Id, validationId)
      p.entry(JsonLdKeywords.Type, (Namespace.Shacl + "NodeShape").iri())

      emitMessage(validation, p)
      emitSeverity(validation, p)
      emitTargetInstance(validation, p)
      emitTargetClass(validation, p)
      emitClosedShape(validation, p)
      emitTargetObject(validation, p)
      emitUnionConstraints(validation, p)
      emitAndConstraints(validation, p)
      emitXoneConstraints(validation, p)
      emitNotConstraint(validation, p)

      validation.functionConstraint.foreach { emitFunctionConstraint(p, validationId, _) }

      emitNodeConstraints(validation, p)
      emitPropertyConstraints(validation, validationId, p)
    }
  }

  private def emitPropertyConstraints(validation: ValidationSpecification, validationId: String, p: EntryBuilder) = {
    if (validation.propertyConstraints.nonEmpty) {
      p.entry(
          (Namespace.Shacl + "property").iri(),
          _.list { b =>
            for {
              constraint <- validation.propertyConstraints
            } yield {
              // processed properties will always include with /prop, this is a CONVENTION
              // can be tricking to follow when debugging
              if (isPropertyConstraintUri(constraint.name)) {
                // These are the standard constraints for AMF/RAML/OAS they have already being sanitised
                emitConstraint(b, constraint.name, constraint)
              } else {
                // this happens when the constraint comes from a profile document
                // an alias for a model element is all the name we provide
                val constraintSegment = if (constraint.name.indexOf("#") > -1) {
                  constraint.name.split("#").last.replace(".", "-")
                } else {
                  constraint.name.replace(".", "-")
                }
                emitConstraint(b, s"$validationId/prop/$constraintSegment", constraint)
              }
            }
          }
      )
    }
  }

  private def emitNodeConstraints(validation: ValidationSpecification, p: EntryBuilder) = {
    for {
      (constraint, values) <- validation.nodeConstraints.groupBy(_.constraint)
    } yield {
      p.entry(Namespace.defaultAliases.expand(constraint).iri(),
              _.list(b => values.foreach(v => link(b, Namespace.defaultAliases.expand(v.value).iri()))))
    }
  }

  private def emitNotConstraint(validation: ValidationSpecification, p: EntryBuilder) = {
    if (validation.notConstraint.isDefined) {
      p.entry((Namespace.Shacl + "not").iri(), _.list { l =>
        link(l, validation.notConstraint.get)
      })
    }
  }

  private def emitXoneConstraints(validation: ValidationSpecification, p: EntryBuilder) = {
    if (validation.xoneConstraints.nonEmpty) {
      p.entry((Namespace.Shacl + "xone").iri(), _.obj {
        _.entry(JsonLdKeywords.List,
                _.list(l =>
                  validation.xoneConstraints.foreach { v =>
                    link(l, v)
                }))
      })
    }
  }

  private def emitAndConstraints(validation: ValidationSpecification, p: EntryBuilder) = {
    if (validation.andConstraints.nonEmpty) {
      p.entry((Namespace.Shacl + "and").iri(), _.obj {
        _.entry(JsonLdKeywords.List,
                _.list(l =>
                  validation.andConstraints.foreach { v =>
                    link(l, v)
                }))
      })
    }
  }

  private def emitUnionConstraints(validation: ValidationSpecification, p: EntryBuilder) = {
    if (validation.unionConstraints.nonEmpty) {
      p.entry((Namespace.Shacl + "or").iri(), _.obj {
        _.entry(JsonLdKeywords.List,
                _.list(l =>
                  validation.unionConstraints.foreach { v =>
                    link(l, v)
                }))
      })
    }
  }

  private def emitTargetObject(validation: ValidationSpecification, p: EntryBuilder) = {
    if (validation.targetObject.nonEmpty)
      p.entry((Namespace.Shacl + "targetObjectsOf").iri(), _.list(p => {
        validation.targetObject.foreach { to =>
          link(p, expandRamlId(to))
        }
      }))
  }

  private def emitClosedShape(validation: ValidationSpecification, p: EntryBuilder) = {
    for {
      closedShape <- validation.closed
    } yield {
      if (closedShape) {
        p.entry((Namespace.Shacl + "closed").iri(), genValue(_, closedShape.toString))
      }
    }
  }

  private def emitTargetClass(validation: ValidationSpecification, p: EntryBuilder) = {
    if (validation.targetClass.nonEmpty)
      p.entry((Namespace.Shacl + "targetClass").iri(), _.list(p => {
        validation.targetClass.foreach { tc =>
          link(p, expandRamlId(tc))
        }
      }))
  }

  private def emitTargetInstance(validation: ValidationSpecification, p: EntryBuilder) = {
    if (validation.targetInstance.nonEmpty)
      p.entry((Namespace.Shacl + "targetNode").iri(), _.list(p => {
        validation.targetInstance.distinct.foreach { ti =>
          link(p, expandRamlId(ti))
        }
      }))
  }

  private def emitMessage(validation: ValidationSpecification, p: EntryBuilder) = {
    val message = getMessageOf(validation)
    if (message != "") {
      p.entry((Namespace.Shacl + "message").iri(), genValue(_, message))
    }
  }

  private def emitSeverity(validation: ValidationSpecification, p: EntryBuilder) = {
    p.entry((Namespace.Shacl + "severity").iri(), genValue(_, validation.severity))
  }

  private def getMessageOf(validation: ValidationSpecification) = {
    targetProfile match {
      case Raml10Profile | Raml08Profile => validation.ramlMessage.getOrElse(validation.message)
      case Oas20Profile                  => validation.oasMessage.getOrElse(validation.message)
      case _                             => validation.message
    }
  }

  private def isPropertyConstraintUri(name: String): Boolean = {
    (name.startsWith("http://") || name.startsWith("https://") || name.startsWith("file:")) &&
    name.indexOf("/prop") > -1
  }

  /**
    * Emits the triples
    * @param parsedPath
    * @return
    */
  def emitPath(b: PartBuilder, parsedPath: PropertyPath): Unit = {
    parsedPath match {
      case PredicatePath(p, true, false) =>
        b.obj { e =>
          e.entry((Namespace.Shacl + "inversePath").iri(), genValue(_, p))
        }

      case PredicatePath(p, false, true) =>
        b.obj { e =>
          e.entry((Namespace.Shacl + "zeroOrMorePath").iri(), genValue(_, p))
        }

      case PredicatePath(p, false, false) =>
        link(b, p)

      case SequencePath(elements) =>
        b.obj { e =>
          e.entry(JsonLdKeywords.List, { l =>
            l.list(p => {
              elements.zipWithIndex.foreach {
                case (e, i) =>
                  emitPath(p, e)
              }
            })
          })
        }
      case AlternatePath(elements) =>
        b.obj { e =>
          e.entry(
              (Namespace.Shacl + "alternativePath").iri(), { e =>
                e.obj { e =>
                  e.entry(JsonLdKeywords.List, { l =>
                    l.list(p => {
                      elements.zipWithIndex.foreach {
                        case (e, i) =>
                          emitPath(p, e)
                      }
                    })
                  })
                }
              }
          )
        }

      case other => throw new Exception(s"""Cannot emit path, unsupported type of path token $other""") // ignore
    }
  }

  /**
    * Builds a path property in a property constraint parsing a provided constraint path
    * @param constraintId
    * @param constraint
    */
  protected def assertPropertyPath(b: EntryBuilder, constraintId: String, constraint: PropertyConstraint): Unit = {
    val parsedPath = constraint.path.get
    b.entry((Namespace.Shacl + "path").iri(), emitPath(_, parsedPath))
  }

  private def emitConstraint(b: PartBuilder, constraintId: String, constraint: PropertyConstraint): Unit = {
    if (Option(constraint.ramlPropertyId).isDefined) {
      b.obj { b =>
        b.entry(JsonLdKeywords.Id, constraintId)

        if (constraint.path.isDefined) {
          assertPropertyPath(b, constraintId, constraint)
        } else {
          b.entry((Namespace.Shacl + "path").iri(), link(_, expandRamlId(constraint.ramlPropertyId)))
        }

        constraint.maxCount.foreach(genPropertyConstraintValue(b, "maxCount", _, Some(constraint)))
        constraint.minCount.foreach(genPropertyConstraintValue(b, "minCount", _, Some(constraint)))
        constraint.maxLength.foreach(genPropertyConstraintValue(b, "maxLength", _, Some(constraint)))
        constraint.minLength.foreach(genPropertyConstraintValue(b, "minLength", _, Some(constraint)))
        constraint.maxExclusive.foreach(genNumericPropertyConstraintValue(b, "maxExclusive", _, Some(constraint)))
        constraint.minExclusive.foreach(genNumericPropertyConstraintValue(b, "minExclusive", _, Some(constraint)))
        constraint.maxInclusive.foreach(genNumericPropertyConstraintValue(b, "maxInclusive", _, Some(constraint)))
        constraint.minInclusive.foreach(genNumericPropertyConstraintValue(b, "minInclusive", _, Some(constraint)))
        if (constraint.atLeast.isDefined) {
          genNumericPropertyConstraintValue(b,
                                            "qualifiedMinCount",
                                            constraint.atLeast.get._1.toString,
                                            Some(constraint))
          b.entry((Namespace.Shacl + "qualifiedValueShape").iri(), link(_, constraint.atLeast.get._2))
        }
        if (constraint.atMost.isDefined) {
          genNumericPropertyConstraintValue(b,
                                            "qualifiedMaxCount",
                                            constraint.atLeast.get._1.toString,
                                            Some(constraint))
          b.entry((Namespace.Shacl + "qualifiedValueShape").iri(), link(_, constraint.atLeast.get._2))
        }
        if (constraint.equalToProperty.isDefined) {
          b.entry((Namespace.Shacl + "equals").iri(), link(_, constraint.equalToProperty.get))
        }
        if (constraint.disjointWithProperty.isDefined) {
          b.entry((Namespace.Shacl + "disjoint").iri(), link(_, constraint.disjointWithProperty.get))
        }
        if (constraint.lessThanProperty.isDefined) {
          b.entry((Namespace.Shacl + "lessThan").iri(), link(_, constraint.lessThanProperty.get))
        }
        if (constraint.lessThanOrEqualsToProperty.isDefined) {
          b.entry((Namespace.Shacl + "lessThanOrEquals").iri(), link(_, constraint.lessThanOrEqualsToProperty.get))
        }
        constraint.multipleOf.foreach(
            genCustomPropertyConstraintValue(b, (Namespace.Shapes + "multipleOfValidationParam").iri(), _))
        constraint.pattern.foreach(v => genPropertyConstraintValue(b, "pattern", v))
        constraint.node.foreach(genPropertyConstraintValue(b, "node", _))
        constraint.value.foreach(genPropertyConstraintValue(b, "hasValue", _, Some(constraint)))
        constraint.datatype.foreach { v =>
          if (!v.endsWith("#float") && !v.endsWith("#number")) {
            // raml/oas 'number' are actually the union of integers and floats
            // i handle the data type integer and float inside of every constraint. Here only need to generate the simples data types for path entry
            b.entry((Namespace.Shacl + "datatype").iri(), link(_, v))
          }
        }
        if (constraint.`class`.nonEmpty) {
          if (constraint.`class`.length == 1) {
            b.entry((Namespace.Shacl + "class").iri(), link(_, constraint.`class`.head))
          } else {
            b.entry(
                (Namespace.Shacl + "or").iri(),
                _.obj {
                  _.entry(JsonLdKeywords.List,
                          _.list(l =>
                            constraint.`class`.foreach { v =>
                              l.obj {
                                _.entry((Namespace.Shacl + "class").iri(), link(_, v))
                              }
                          }))
                }
            )
          }
        }

        // custom builder
        constraint.custom.foreach { builder =>
          builder(b, constraintId)
        }

        if (constraint.in.nonEmpty) {
          b.entry(
              (Namespace.Shacl + "in").iri(),
              _.obj {
                _.entry(JsonLdKeywords.List, _.list(b => constraint.in.foreach(genValue(b, _))))
              }
          )
        }
      }
    }
  }

  private def emitFunctionConstraint(b: EntryBuilder, validationId: String, f: FunctionConstraint): Unit = {
    genJSValidator(validationId, f)
    genJSConstraint(validationId, f)
    b.entry(f.validatorPath(validationId), genValue(_, "true"))
  }

  private def genJSConstraint(validationId: String, f: FunctionConstraint) = {
    val constraintId  = f.constraintId(validationId)
    val validatorId   = f.validatorId(validationId)
    val validatorPath = f.validatorPath(validationId)

    jsConstraintEmitters += new PartEmitter {
      override def emit(b: PartBuilder): Unit = {
        b.obj { b =>
          b.entry(JsonLdKeywords.Id, constraintId)
          b.entry(JsonLdKeywords.Type, (Namespace.Shacl + "ConstraintComponent").iri())
          if (f.parameters.nonEmpty) {
            b.entry(
                (Namespace.Shacl + "parameter").iri(),
                _.obj { b =>
                  b.entry(
                      (Namespace.Shacl + "path").iri(),
                      _.obj(_.entry(JsonLdKeywords.Id, f.parameters.head.path))
                  )
                  b.entry(
                      (Namespace.Shacl + "datatype").iri(),
                      _.obj(_.entry(JsonLdKeywords.Id, f.parameters.head.datatype))
                  )
                }
            )
          } else {
            b.entry(
                (Namespace.Shacl + "parameter").iri(),
                _.obj { b =>
                  b.entry(
                      (Namespace.Shacl + "path").iri(),
                      _.obj(_.entry(JsonLdKeywords.Id, validatorPath))
                  )
                  b.entry(
                      (Namespace.Shacl + "datatype").iri(),
                      _.obj(_.entry(JsonLdKeywords.Id, DataType.Boolean))
                  )
                }
            )
          }
          b.entry((Namespace.Shacl + "validator").iri(), _.obj(_.entry(JsonLdKeywords.Id, validatorId)))
        }
      }
      override def position(): Position = Position.ZERO
    }
  }

  private def genJSValidator(validationId: String, f: FunctionConstraint) = {
    val validatorId = f.validatorId(validationId)
    jsValidatorEmitters += new PartEmitter {
      override def emit(b: PartBuilder): Unit = {
        f.functionName match {
          case Some(fnName) =>
            b.obj { b =>
              b.entry(JsonLdKeywords.Id, validatorId)
              b.entry(JsonLdKeywords.Type, (Namespace.Shacl + "JSValidator").iri())
              f.message.foreach(msg => b.entry((Namespace.Shacl + "message").iri(), genValue(_, msg)))
              b.entry(
                  (Namespace.Shacl + "jsLibrary").iri(),
                  _.list { b =>
                    for { library <- f.libraries } {
                      b.obj {
                        _.entry(
                            (Namespace.Shacl + "jsLibraryURL").iri(),
                            _.obj { o =>
                              o.entry(JsonLdKeywords.Value, library)
                              o.entry(JsonLdKeywords.Type, "http://www.w3.org/2001/XMLSchema#anyUri")
                            }
                        )
                      }
                    }
                  }
              )
              b.entry((Namespace.Shacl + "jsFunctionName").iri(), genValue(_, fnName))
            }

          case None =>
            f.code match {
              case Some(_) =>
                b.obj { b =>
                  b.entry(JsonLdKeywords.Id, validatorId)
                  b.entry(JsonLdKeywords.Type, (Namespace.Shacl + "JSValidator").iri())
                  f.message.foreach(msg => b.entry((Namespace.Shacl + "message").iri(), genValue(_, msg)))
                  b.entry(
                      (Namespace.Shacl + "jsLibrary").iri(),
                      _.list { b =>
                        for { library <- Seq(ShaclJsonLdShapeGraphEmitter.validationLibraryUrl) ++ f.libraries } {
                          b.obj {
                            _.entry(
                                (Namespace.Shacl + "jsLibraryURL").iri(),
                                _.obj { o =>
                                  o.entry(JsonLdKeywords.Value, library)
                                  o.entry(JsonLdKeywords.Type, "http://www.w3.org/2001/XMLSchema#anyUri")
                                }
                            )
                          }
                        }
                      }
                  )
                  b.entry((Namespace.Shacl + "jsFunctionName").iri(), genValue(_, f.computeFunctionName(validationId)))
                }
              case _ => throw new Exception("Cannot emit validator without JS code or JS function name")
            }
        }
      }
      override def position(): Position = Position.ZERO
    }
  }

  private def genPropertyConstraintValue(b: EntryBuilder,
                                         constraintName: String,
                                         value: String,
                                         constraint: Option[PropertyConstraint] = None): Unit = {
    constraint.flatMap(_.datatype) match {
      case Some(scalarType) if scalarType == (Namespace.Shapes + "number").iri() =>
        b.entry(
            (Namespace.Shacl + "or").iri(),
            _.obj {
              _.entry(
                  JsonLdKeywords.List,
                  _.list { l =>
                    l.obj { o =>
                      o.entry((Namespace.Shacl + constraintName).iri(),
                              genValue(_, value.toDouble.floor.toInt.toString, Some(DataType.Integer)))
                    }
                    l.obj { o =>
                      o.entry((Namespace.Shacl + constraintName).iri(),
                              genValue(_, value.toDouble.toString, Some(DataType.Double)))
                    }
                  }
              )
            }
        )
      case Some(_) =>
        b.entry((Namespace.Shacl + constraintName).iri(), genValue(_, value, constraint.flatMap(_.datatype)))
      case None =>
        b.entry((Namespace.Shacl + constraintName).iri(), genValue(_, value, None))
    }

  }

  private def genCustomPropertyConstraintValue(b: EntryBuilder, constraintIri: String, value: String): Unit = {
    b.entry(constraintIri, value)
  }

  private def genNumericPropertyConstraintValue(b: EntryBuilder,
                                                constraintName: String,
                                                value: String,
                                                constraint: Option[PropertyConstraint] = None): Unit = {
    constraint.flatMap(_.datatype) match {
      case Some(scalarType) if scalarType == (Namespace.Shapes + "number").iri() || scalarType == DataType.Double =>
        b.entry((Namespace.Shacl + constraintName).iri(), genValue(_, value.toDouble.toString, Some(DataType.Double)))
      case None =>
        b.entry((Namespace.Shacl + constraintName).iri(), genValue(_, value.toDouble.toString, Some(DataType.Double)))
      case Some(scalarType) if scalarType == DataType.Float =>
        b.entry((Namespace.Shacl + constraintName).iri(), genValue(_, value.toDouble.toString, Some(DataType.Float)))
      case Some(scalarType) if scalarType == DataType.Integer =>
        b.entry((Namespace.Shacl + constraintName).iri(),
                genValue(_, value.toDouble.floor.toInt.toString, Some(DataType.Integer)))

    }
  }

  private def expandRamlId(s: String): String =
    if (s.startsWith("http://") || s.startsWith("https://") || s.startsWith("file:")) {
      s.trim
    } else {
      Namespace.defaultAliases.expand(s.replace(".", ":")).iri().trim
    }

  private def genNonEmptyList(b: PartBuilder): Unit = {
    b.obj { b =>
      b.entry(JsonLdKeywords.Type, raw(_, (Namespace.Shacl + "NodeShape").iri()))
      b.entry((Namespace.Shacl + "message").iri(), raw(_, "List cannot be empty"))
      b.entry(
          (Namespace.Shacl + "property").iri(),
          _.list {
            _.obj { b =>
              b.entry((Namespace.Shacl + "path").iri(), link(_, (Namespace.Rdfs + "_1").iri()))
              b.entry(
                  (Namespace.Shacl + "minCount").iri(),
                  _.obj {
                    _.entry(JsonLdKeywords.Value, raw(_, "1", YType.Int))
                  }
              )
            }
          }
      )
    }
  }

  private def genValue(b: PartBuilder, s: String, dType: Option[String] = None): Unit = {
    dType match {
      case Some(dt) =>
        b.obj(p => {
          p.entry(JsonLdKeywords.Value, s)
          p.entry(JsonLdKeywords.Type, dt)
        })
      case None =>
        if (s.matches("^-?[1-9]\\d*$|^0$")) { // if the number starts with 0 (and is not 0), its a string and should be quoted
          b.obj(_.entry(JsonLdKeywords.Value, raw(_, s, YType.Int)))
        } else if (s == "true" || s == "false") {
          b.obj(_.entry(JsonLdKeywords.Value, raw(_, s, YType.Bool)))
        } else if (Namespace.defaultAliases
                     .expand(s)
                     .iri() == Namespace.defaultAliases.expand("amf-parser:NonEmptyList").iri()) {
          genNonEmptyList(b)
        } else if (s.startsWith("http://") || s.startsWith("https://") || s.startsWith("file:")) {
          link(b, s)
        } else {
          b.obj(b => b.entry(JsonLdKeywords.Value, s))
        }
    }
  }
}

object ShaclJsonLdShapeGraphEmitter {
  def validationLibraryUrl: String = (Namespace.AmfParser + "validationLibrary.js").iri()
}
