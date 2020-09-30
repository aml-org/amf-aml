package amf.plugins.document.vocabularies.parser.dialects

import amf.core.model.domain.{AmfArray, AmfScalar}
import amf.core.parser.{Annotations, ScalarNode, SearchScope}
import amf.plugins.document.vocabularies.model.domain.{DocumentMapping, PublicNodeMapping}
import org.yaml.model.{YMap, YMapEntry, YScalar}
import amf.core.utils._
import amf.plugins.document.vocabularies.metamodel.domain.{DocumentMappingModel, PublicNodeMappingModel}
import amf.core.parser._
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
