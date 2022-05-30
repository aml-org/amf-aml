package amf.aml.internal.parse.instances.parser.applicable

import amf.aml.client.scala.model.domain.{AnyMapping, NodeMapping}
import amf.aml.internal.parse.instances.DialectInstanceContext
import amf.aml.internal.parse.instances.parser.{AmlSubGraphCollector, InstanceElementParser}
import amf.core.client.common.validation.AMFStyle
import amf.core.client.scala.errorhandling.DefaultErrorHandler
import amf.core.client.scala.validation.AMFValidationResult
import amf.core.internal.parser.Root
import amf.validation.internal.shacl.custom.CustomShaclValidator
import org.yaml.model.YMap

object NodePropertyConforms {
  def conformsAgainstProperties(map: YMap, mapping: AnyMapping, root: Root)(implicit
      ctx: DialectInstanceContext
  ): Boolean = {
    mapping match {
      case nodeMapping: NodeMapping if nodeMapping.propertiesMapping().nonEmpty =>
        parseAndValidate(map, nodeMapping, root)
      case _ => false
    }
  }

  private def parseAndValidate(map: YMap, mapping: NodeMapping, root: Root)(implicit
      ctx: DialectInstanceContext
  ): Boolean = {
    val nextContext = ctx.copy(DefaultErrorHandler())
    val element =
      InstanceElementParser(root).parse("", mapping.id, map, mapping, Map.empty, parseAllOf = false)(nextContext)
    val conforms = ignoreClosedShapeErrors(nextContext.eh.getResults).isEmpty
    if (!conforms) return false
    val mappingsInTree = AmlSubGraphCollector.collect(mapping.id, ctx.dialect)
    val validator      = new CustomShaclValidator(Map.empty, AMFStyle)
    val validations = ctx.constraints
      .map(p => p.validations.filter(x => x.targetClass.intersect(mappingsInTree).nonEmpty))
      .getOrElse(Nil)
      .toList
    val report = validator.validate(element, validations)
    report.conforms
  }

  private def ignoreClosedShapeErrors(results: Seq[AMFValidationResult]): Seq[AMFValidationResult] = {
    results.filterNot(_.validationId.contains("closed"))
  }
}
