package amf.client.environment

import amf.client.parse.DefaultParserErrorHandler
import amf.client.remod.AMFEnvironment
import amf.client.remod.parsing.{AMLDialectInstanceParsingPlugin, AMLDialectParsingPlugin, AMLVocabularyParsingPlugin}
import amf.client.remod.rendering.{
  AMLDialectInstanceRenderingPlugin,
  AMLDialectRenderingPlugin,
  AMLVocabularyRenderingPlugin
}
import amf.core.unsafe.PlatformSecrets
import amf.core.{AMFCompiler, CompilerContextBuilder}
import amf.plugins.document.graph.{AMFGraphParsePlugin, AMFGraphRenderPlugin}
import amf.plugins.document.vocabularies.model.document.Dialect

import scala.concurrent.{ExecutionContext, Future}

private[amf] object AmlEnvironment extends PlatformSecrets {

  def aml(): AMFEnvironment =
    AMFEnvironment
      .default()
      .withPlugins(
          List(
              new AMLDialectParsingPlugin(),
              new AMLVocabularyParsingPlugin(),
              new AMLDialectRenderingPlugin(),
              new AMLVocabularyRenderingPlugin(),
              AMFGraphParsePlugin,
              AMFGraphRenderPlugin
          ))

  // TODO: what about nested $dialect references?
  def forInstance(url: String, mediaType: Option[String] = None)(
      implicit e: ExecutionContext): Future[AMFEnvironment] = {
    var env      = aml()
    val ctx      = new CompilerContextBuilder(url, platform, eh = DefaultParserErrorHandler.withRun()).build()
    val compiler = new AMFCompiler(ctx, mediaType, None)
    for {
      content                <- compiler.fetchContent()
      eitherContentOrAst     <- Future.successful(compiler.parseSyntax(content))
      root                   <- Future.successful(eitherContentOrAst.right.get) if eitherContentOrAst.isRight
      plugin                 <- Future.successful(compiler.getDomainPluginFor(root))
      documentWithReferences <- compiler.parseReferences(root, plugin.get) if plugin.isDefined
    } yield {
      documentWithReferences.references.foreach { r =>
        r.unit match {
          case d: Dialect =>
            val parsing: AMLDialectInstanceParsingPlugin     = new AMLDialectInstanceParsingPlugin(d)
            val rendering: AMLDialectInstanceRenderingPlugin = new AMLDialectInstanceRenderingPlugin(d)
            env = env.withPlugins(List(parsing, rendering))
          case _ => // Ignore
        }
      }
      env
    }
  }

}
