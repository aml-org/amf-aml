package amf.plugins.document.vocabularies.model.domain
import amf.core.model.StrField
import amf.core.model.domain.{AmfScalar, DomainElement}
import amf.core.parser.Annotations
import amf.plugins.document.vocabularies.metamodel.domain.NodeWithDiscriminatorModel

trait NodeWithDiscriminator[M <: NodeWithDiscriminatorModel] extends DomainElement with HasObjectRange[M] {

  def typeDiscriminatorName(): StrField = fields.field(meta.TypeDiscriminatorName)
  def typeDiscriminator(): Map[String, String] =
    Option(fields(meta.TypeDiscriminator)).map { disambiguator: String =>
      disambiguator.split(",").foldLeft(Map[String, String]()) {
        case (acc, typeMapping) =>
          val pair = typeMapping.split("->")
          acc + (pair.lift(1).getOrElse("") -> pair(0))
      }
    }.orNull

  def withTypeDiscriminatorName(name: String): this.type = set(meta.TypeDiscriminatorName, name)
  def withTypeDiscriminator(typesMapping: Map[String, String],
                            entryAnnotations: Annotations = Annotations(),
                            valueAnnotations: Annotations = Annotations()): this.type =
    set(meta.TypeDiscriminator,
        AmfScalar(typesMapping.map { case (a, b) => s"$a->$b" }.mkString(","), valueAnnotations),
        entryAnnotations)

  override def meta: M

}
