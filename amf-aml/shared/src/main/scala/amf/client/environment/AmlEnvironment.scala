package amf.client.environment

import amf.client.remod.AMFEnvironment
import amf.plugins.document.graph.{AMFGraphParsePlugin, AMFGraphRenderPlugin}
import amf.plugins.document.vocabularies.{AMLParsePlugin, AMLRenderPlugin}

private[amf] object AmlEnvironment {

  def aml(): AMFEnvironment =
    AMFEnvironment
      .default()
      .withPlugins(List(AMLParsePlugin, AMLRenderPlugin, AMFGraphParsePlugin, AMFGraphRenderPlugin))

}
