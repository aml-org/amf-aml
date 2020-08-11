package amf.plugins.document.vocabularies.emitters.dialects

import amf.plugins.document.vocabularies.emitters.instances.AmlEmittersHelper
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.model.domain.NodeMappable

trait AliasesConsumer extends AmlEmittersHelper {
  val dialect: Dialect
  val aliases: Map[String, (String, String)]
  def aliasFor(id: String): Option[String] = {
    if (Option(id).isEmpty) {
      None
    }
    else {
      maybeFindNodeMappingById(id) match {
        case Some((_, nodeMapping: NodeMappable)) =>
          if (id.startsWith(dialect.id)) {
            Some(nodeMapping.name.value())
          }
          else {
            val matchingAliases = aliases.keySet.filter(id.contains(_))
            // we pick the most specific (longer) matching URI
            matchingAliases.toList.sorted.reverse.headOption.map { key =>
              val alias = aliases(key)._1
              alias + "." + nodeMapping.name.value()
            } orElse {
              Some(nodeMapping.name.value())
            }
          }

        case _ =>
          if (id.startsWith(dialect.id)) {
            // local reference
            Some(id.split(dialect.id).last.replace("/declarations/", ""))
          }
          else {
            aliases.keySet.find(id.contains(_)) map { key =>
              val alias = aliases(key)._1
              val postfix = id.split(key).last match {
                case i if i.contains("/declarations/") => i.replace("/declarations/", "")
                case nonLibraryDeclaration             => nonLibraryDeclaration
              }
              if (postfix.startsWith("#")) {
                alias + "." + postfix.drop(1) // recursive references
              }
              else {
                alias + "." + postfix
              }
            }
          }
      }
    }
  }
}
