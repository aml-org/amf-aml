package amf.aml.internal.render.emitters.instances
import amf.aml.internal.metamodel.domain.NodeWithDiscriminatorModel
import amf.aml.client.scala.model.domain.{DialectDomainElement, NodeMapping, NodeWithDiscriminator, UnionNodeMapping}

case class DiscriminatorHelper(
    mapping: NodeWithDiscriminator[_ <: NodeWithDiscriminatorModel],
    dialectEmitter: AmlEmittersHelper
) {
  // maybe we have a discriminator
  val discriminator: Option[Map[String, String]] =
    Option(mapping.typeDiscriminator()).orElse {
      val rangeId = mapping.objectRange().head.value()
      dialectEmitter.findNodeMappingById(rangeId) match {
        case (_, unionMapping: UnionNodeMapping) =>
          Option(unionMapping.typeDiscriminator())
        case _ => None
      }
    }

  // maybe we have a discriminator name
  val discriminatorName: Option[String] =
    mapping.typeDiscriminatorName().option().orElse {
      val rangeId = mapping.objectRange().head.value()
      dialectEmitter.findNodeMappingById(rangeId) match {
        case (_, unionMapping: UnionNodeMapping) =>
          unionMapping.typeDiscriminatorName().option()
        case _ => None
      }
    }

  // we build the discriminator mapping if we have a discriminator
  val discriminatorMappings: Map[String, NodeMapping] =
    discriminator.getOrElse(Map()).foldLeft(Map[String, NodeMapping]()) { case (acc, (alias, mappingId)) =>
      dialectEmitter.findNodeMappingById(mappingId) match {
        case (_, nodeMapping: NodeMapping) => acc + (alias -> nodeMapping)
        case _                             => acc // TODO: violation here
      }
    }

  def compute(dialectDomainElement: DialectDomainElement): Option[(String, String)] = {
    val elementTypes = dialectDomainElement.meta.`type`.map(_.iri())
    discriminatorMappings.find { case (_, discriminatorMapping) =>
      elementTypes.contains(discriminatorMapping.nodetypeMapping.value())
    } match {
      case Some((alias, _)) =>
        Some((discriminatorName.getOrElse("type"), alias))
      case _ =>
        None
    }
  }
}
