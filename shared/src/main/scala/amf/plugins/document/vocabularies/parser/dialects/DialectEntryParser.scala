package amf.plugins.document.vocabularies.parser.dialects

import org.yaml.model.{YMap, YMapEntry}
import amf.core.parser.YMapOps

abstract class DialectEntryParser()( implicit val ctx:DialectContext) {
  def parse(entry:YMapEntry): Unit
}

object DialectAstOps {

  implicit class DialectYMapOps(map: YMap) extends YMapOps(map) {
    def parse(keyword:String, parser: DialectEntryParser): Unit = key(keyword,parser.parse)
  }

}
