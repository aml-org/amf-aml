package amf.aml.internal.parse.instances

import amf.core.internal.parser.{Root, YMapOps}
import amf.aml.client.scala.model.document.DialectInstancePatch
import amf.aml.internal.validate.DialectValidations.DialectError
import org.yaml.model.YType

class DialectInstancePatchParser(root: Root)(implicit override val ctx: DialectInstanceContext)
    extends DialectInstanceParser(root) {

  def parse(): DialectInstancePatch = {
    val dialectInstance = parseDocument()
    val patch           = DialectInstancePatch(dialectInstance.fields, dialectInstance.annotations)
    patch.withId(dialectInstance.id)
    checkTarget(patch)
  }

  private def checkTarget(patch: DialectInstancePatch): DialectInstancePatch = {
    map.key("$target") match {
      case Some(entry) if entry.value.tagType == YType.Str =>
        patch.withExtendsModel(platform.resolvePath(entry.value.as[String]))

      case Some(entry) =>
        ctx.eh.violation(DialectError, patch.id, "Patch $target must be a valid URL", entry.value)

      case _ => // ignore
    }
    patch
  }
}
