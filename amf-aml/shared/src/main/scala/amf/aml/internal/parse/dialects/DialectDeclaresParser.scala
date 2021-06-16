package amf.aml.internal.parse.dialects

import amf.core.client.scala.model.domain.{AmfArray, AmfScalar}
import amf.core.internal.parser.YMapOps
import amf.core.internal.parser.domain.{Annotations, ScalarNode, SearchScope}
import amf.core.internal.utils.AmfStrings
import amf.aml.internal.metamodel.domain.{DocumentMappingModel, PublicNodeMappingModel}
import amf.aml.client.scala.model.domain.{DocumentMapping, PublicNodeMapping}
import org.yaml.model.{YMap, YMapEntry, YScalar}
case class DialectDeclaresParser(into: DocumentMapping)(override implicit val ctx: DialectContext)
    extends DialectEntryParser {

  def parse(map: YMap): Unit = map.key("declares", parse)

  override def parse(entry: YMapEntry): Unit = {
    val declaresMap = entry.value.as[YMap]
    val declarations: Seq[PublicNodeMapping] = declaresMap.entries.map { declarationEntry =>
      val declarationId   = declarationEntry.value.as[YScalar].text
      val declarationName = ScalarNode(declarationEntry.key).string()
      val declarationMapping = PublicNodeMapping(declarationEntry)
        .set(PublicNodeMappingModel.Name, declarationName, Annotations(declarationEntry.key))
        .withId(into.id + "/declaration/" + declarationName.toString.urlComponentEncoded)
      val nodeMapping = ctx.declarations.findNodeMappingOrError(entry.value)(declarationId, SearchScope.All)
      declarationMapping.set(PublicNodeMappingModel.MappedNode,
                             AmfScalar(nodeMapping.id, Annotations(declarationEntry.value)),
                             Annotations(declarationEntry.value))

    }
    into.set(DocumentMappingModel.DeclaredNodes, AmfArray(declarations, Annotations(entry.value)), Annotations(entry))
  }
}
