package amf.plugins.document.vocabularies.parser.instances

import amf.core.Root
import amf.plugins.document.vocabularies.model.document.DialectInstancePatch
import amf.validation.DialectValidations.DialectError
import org.yaml.model.YType
import amf.core.parser._

class DialectInstancePatchParser(root: Root)(implicit override val ctx: DialectInstanceContext) extends DialectInstanceParser(root) {

  def parse(): Option[DialectInstancePatch] = {
    parseDocument() match {
      case Some(dialectInstance) =>
        val patch = DialectInstancePatch(dialectInstance.fields, dialectInstance.annotations)
        patch.withId(dialectInstance.id)
        Some(checkTarget(patch))
      case _ =>
        None
    }
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
