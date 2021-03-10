package amf.client.environment

import amf.client.remod.AMFEnvironment
import amf.plugins.document.graph.AMFGraphParsePlugin
import amf.plugins.document.vocabularies.AMLParsePlugin

private[amf] object AmlEnvironment {

  def aml(): AMFEnvironment = AMFEnvironment.default().withPlugins(List(AMLParsePlugin, AMFGraphParsePlugin))

}
