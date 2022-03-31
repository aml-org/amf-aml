package amf.rdf.internal.plugins

import amf.core.client.common.{LowPriority, PluginPriority}
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.plugins.render.{AMFRenderPlugin, RenderConfiguration, RenderInfo}
import amf.core.internal.plugins.syntax.{ASTBuilder, StringDocBuilder}
import amf.core.internal.remote.Mimes
import amf.rdf.client.scala.{RdfConfiguration, RdfUnitConverter}

object RdfRenderPlugin extends AMFRenderPlugin {

  override val id: String = "rdf-render-plugin"

  override def defaultSyntax(): String = Mimes.`text/n3`

  override def mediaTypes: Seq[String] = Seq(Mimes.`text/n3`)

  override def applies(element: RenderInfo): Boolean = true

  override def priority: PluginPriority = LowPriority

  override def getDefaultBuilder: ASTBuilder[_] = new StringDocBuilder()

  override def emit[T](unit: BaseUnit,
                       builder: ASTBuilder[T],
                       renderConfiguration: RenderConfiguration,
                       mediaType: String): Boolean = {
    builder match {
      case stringDocBuilder: StringDocBuilder =>
        val rdfModel =
          RdfUnitConverter.toNativeRdfModel(unit, RdfConfiguration(), renderConfiguration.renderOptions)
        stringDocBuilder += rdfModel.toN3()
        true
      case _ => false
    }
  }
}
