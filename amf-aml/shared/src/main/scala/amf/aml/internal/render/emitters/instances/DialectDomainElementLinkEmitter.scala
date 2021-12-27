package amf.aml.internal.render.emitters.instances

import amf.aml.client.scala.model.document.DialectInstanceFragment
import amf.aml.client.scala.model.domain.DialectDomainElement
import amf.aml.internal.annotations.{JsonPointerRef, RefInclude}
import amf.core.client.common.position.Position
import amf.core.client.common.position.Position.ZERO
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.annotations.{LexicalInformation, SourceNode}
import amf.core.internal.render.BaseEmitters.TextScalarEmitter
import amf.core.internal.render.emitters.PartEmitter
import org.yaml.model.YDocument.PartBuilder
import org.yaml.model.{YNode, YType}

case class DialectDomainElementLinkEmitter(node: DialectDomainElement, references: Seq[BaseUnit]) extends PartEmitter {
  override def emit(b: PartBuilder): Unit = {
    if (node.annotations.contains(classOf[RefInclude])) {
      b.obj { m =>
        m.entry("$include", node.includeName)
      }
    } else if (node.annotations.contains(classOf[JsonPointerRef])) {
      b.obj { m =>
        m.entry("$ref", node.linkLabel.option().getOrElse(node.linkTarget.get.id))
      }
    } else if (isFragmentRef(node, references)) {
      b += YNode.include(node.includeName)
    } else {
      // case of library and declaration references
      TextScalarEmitter(node.linkLabel.value(), node.annotations).emit(b)
    }
  }

  def isFragmentRef(elem: DialectDomainElement, references: Seq[BaseUnit]): Boolean = {
    elem.annotations.find(classOf[SourceNode]) match {
      case Some(SourceNode(node)) => node.tagType == YType.Include
      case None if references.nonEmpty =>
        elem.linkTarget match {
          case Some(domainElement) =>
            references.exists {
              case ref: DialectInstanceFragment =>
                ref.encodes.id == domainElement.id
              case _ => false
            }
          case _ =>
            throw new Exception(s"Cannot check fragment for an element without target for element ${elem.id}")
        }
      case _ => false
    }
  }

  override def position(): Position =
    node.annotations
      .find(classOf[LexicalInformation])
      .map(_.range.start)
      .getOrElse(ZERO)
}
