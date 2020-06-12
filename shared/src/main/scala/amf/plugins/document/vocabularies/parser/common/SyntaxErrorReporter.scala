package amf.plugins.document.vocabularies.parser.common

import amf.core.annotations.{LexicalInformation, SourceLocation}
import amf.core.parser.{Annotations, ParserContext, Range}
import amf.core.utils.AmfStrings
import amf.plugins.document.vocabularies.metamodel.domain.PropertyMappingModel
import amf.plugins.document.vocabularies.model.domain.PropertyMapping
import amf.validation.DialectValidations._
import org.yaml.model.{YNode, YPart}

trait SyntaxErrorReporter { this: ParserContext =>

  def missingTermViolation(term: String, node: String, ast: YPart): Unit = {
    eh.violation(MissingTermSpecification, node, s"Cannot find class vocabulary term $term", ast)
  }

  def missingTermWarning(term: String, node: String, ast: YPart): Unit = {
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
