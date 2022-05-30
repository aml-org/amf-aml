package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain.NodeMapping
import amf.aml.internal.annotations.JsonPointerRef
import amf.aml.internal.parse.instances.{DialectInstanceContext, JsonPointerResolver, NodeMappableHelper}
import amf.core.internal.parser.Root
import org.yaml.model.YMap

object RefNodeParser extends NodeMappableHelper {

  def parse(defaultId: String, astMap: YMap, mappable: NodeMappable, root: Root)(implicit
      ctx: DialectInstanceContext
  ) = {
    val ref = JsonPointerResolver.resolveJSONPointer(astMap, mappable, defaultId, root)
    ref.annotations += JsonPointerRef()
    mappable match {
      case m: NodeMapping => ref.withDefinedBy(m)
      case _              => // ignore
    }
    ref
  }
}
