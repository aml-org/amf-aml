package amf.aml.internal.parse.dialects

import amf.core.client.scala.model.domain.{AmfObject, AmfScalar}
import amf.core.internal.metamodel.{Field, Type}
import amf.core.internal.parser.YMapOps
import amf.core.internal.parser.domain.{Annotations, ScalarNode}
import org.yaml.model.{YMap, YMapEntry, YNode, YType}

abstract class DialectEntryParser()(implicit val ctx: DialectContext) {
  def parse(entry: YMapEntry): Unit
}

object DialectAstOps {

  implicit class DialectYMapOps(map: YMap) extends YMapOps(map) {

    override def key(keyword: String, fn: YMapEntry => Unit): Unit = super.key(keyword, fn)

    def parse(keyword: String, parser: DialectEntryParser): Unit = key(keyword, parser.parse)
  }

  implicit class DialectScalarValueEntryParserOpts(target: AmfObject)(implicit val ctx: DialectContext) {
    def setParsing(f: Field): DialectEntryParser = new DialectScalarValueEntryParser(f, target)(ctx)
  }
}

class DialectScalarValueEntryParser(f: Field, target: AmfObject)(override implicit val ctx: DialectContext)
    extends DialectEntryParser {

  override def parse(entry: YMapEntry): Unit = target.set(f, buildScalarNode(entry.value), Annotations(entry))

  protected def buildScalarNode(node: YNode): AmfScalar = typedScalar(ScalarNode(node), node.tagType)

  private def typedScalar(scalar: ScalarNode, tagType: YType): AmfScalar = {
    f.`type` match {
      case Type.Int                         => scalar.integer()
      case Type.Bool                        => scalar.boolean()
      case Type.Double                      => scalar.double()
      case Type.Str if tagType == YType.Str => scalar.string()
      case _                                => scalar.text()
    }
  }
}
