package amf.plugins.document.vocabularies.model.domain
import amf.core.model.StrField
import amf.core.model.domain.{AmfScalar, DomainElement}
import amf.core.parser.Annotations
import amf.plugins.document.vocabularies.metamodel.domain.UnionNodeMappingModel._

trait NodeWithDiscriminator[T] extends DomainElement {
  def objectRange(): Seq[StrField]      = fields.field(ObjectRange)
  def typeDiscriminatorName(): StrField = fields.field(TypeDiscriminatorName)
  def typeDiscriminator(): Map[String, String] =
    Option(fields(TypeDiscriminator)).map { disambiguator: String =>
      disambiguator.split(",").foldLeft(Map[String, String]()) {
        case (acc, typeMapping) =>
          val pair = typeMapping.split("->")
          acc + (pair.lift(1).getOrElse("") -> pair(0))
      }
    }.orNull

  def withObjectRange(range: Seq[String]): T     = set(ObjectRange, range).asInstanceOf[T]
  def withTypeDiscriminatorName(name: String): T = set(TypeDiscriminatorName, name).asInstanceOf[T]
  def withTypeDiscriminator(typesMapping: Map[String, String],
                            entryAnnotations: Annotations = Annotations(),
                            valueAnnotations: Annotations = Annotations()): T =
    set(TypeDiscriminator,
        AmfScalar(typesMapping.map { case (a, b) => s"$a->$b" }.mkString(","), valueAnnotations),
        entryAnnotations)
      .asInstanceOf[T]
}
