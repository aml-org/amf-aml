package amf.aml.internal.parse.common

import amf.core.client.scala.parse.document.ParserContext
import amf.core.internal.annotations.{LexicalInformation, SourceLocation}
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.utils.AmfStrings
import amf.aml.internal.metamodel.domain.PropertyMappingModel
import amf.aml.client.scala.model.domain.PropertyMapping
import amf.aml.internal.validate.DialectValidations._
import org.yaml.model.{YNode, YPart}
import amf.core.client.common.position.Range

trait SyntaxErrorReporter { this: ParserContext =>

  def missingClassTermWarning(term: String, node: String, ast: YPart): Unit = {
    eh.warning(MissingClassTermSpecification, node, s"Cannot find class vocabulary term $term", ast)
  }

  def missingPropertyTermWarning(term: String, node: String, ast: YPart): Unit = {
    eh.warning(MissingPropertyTermSpecification, node, s"Cannot find property vocabulary term $term", ast)
  }

  def missingFragmentViolation(fragment: String, node: String, ast: YPart): Unit = {
    eh.violation(MissingFragmentSpecification, node, s"Cannot find fragment $fragment", ast)
  }

  def missingPropertyRangeViolation(term: String, node: String, annotations: Annotations): Unit = {
    eh.violation(
        MissingPropertyRangeSpecification,
        node,
        Some(PropertyMappingModel.ObjectRange.value.iri()),
        s"Cannot find property range term $term",
        annotations.find(classOf[LexicalInformation]),
        annotations.find(classOf[SourceLocation]).map(_.location)
    )
  }

  def missingPropertyKeyViolation(node: String, field: String, label: String, annotations: Annotations): Unit = {
    eh.violation(
        MissingPropertyRangeSpecification,
        node,
        Some(field),
        s"Cannot find property $label in mapping range",
        annotations.find(classOf[LexicalInformation]),
        annotations.find(classOf[SourceLocation]).map(_.location)
    )
  }

  def differentTermsInMapKey(node: String, field: String, label: String, annotations: Annotations): Unit = {
    eh.violation(
        DifferentTermsInMapKey,
        node,
        Some(field),
        s"Cannot find property $label in mapping range",
        annotations.find(classOf[LexicalInformation]),
        annotations.find(classOf[SourceLocation]).map(_.location)
    )
  }

  def inconsistentPropertyRangeValueViolation(node: String,
                                              property: PropertyMapping,
                                              expected: String,
                                              found: String,
                                              valueNode: YNode): Unit = {
    eh.violation(
        InconsistentPropertyRangeValueSpecification,
        node,
        Some(property.nodePropertyMapping().value()),
        s"Cannot find expected range for property ${property.nodePropertyMapping().value()} (${property.name().value()}). Found '$found', expected '$expected'",
        Some(new LexicalInformation(Range(valueNode.range))),
        valueNode.sourceName.option
    )

  }

  def closedNodeViolation(id: String, property: String, nodeType: String, ast: YPart): Unit = {
    eh.violation(
        ClosedShapeSpecification,
        id,
        s"Property: '$property' not supported in a $nodeType node",
        ast
    )
  }

  def missingPropertyViolation(id: String, property: String, nodeType: String, ast: YPart): Unit = {
    eh.violation(
        MissingPropertySpecification,
        id,
        s"Property: '$property' mandatory in a $nodeType node",
        ast
    )
  }
}
