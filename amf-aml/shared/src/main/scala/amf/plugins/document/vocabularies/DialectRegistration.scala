package amf.plugins.document.vocabularies

import amf.client.environment.AMLConfiguration
import amf.client.execution.BaseExecutionEnvironment
import amf.client.errorhandling.DefaultErrorHandler
import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.client.remod.namespace.AMLDialectNamespaceAliasesPlugin
import amf.client.remod.parsing.AMLDialectInstanceParsingPlugin
import amf.client.remod.rendering.AMLDialectInstanceRenderingPlugin
import amf.core.metamodel.domain.{ModelDoc, ModelVocabularies}
import amf.core.metamodel.{Field, Type}
import amf.core.resolution.pipelines.TransformationPipelineRunner
import amf.core.vocabulary.ValueType
import amf.internal.environment.Environment
import amf.internal.resource.StringResourceLoader
import amf.plugins.document.vocabularies.emitters.instances.DefaultNodeMappableFinder
import amf.plugins.document.vocabularies.metamodel.domain.DialectDomainElementModel
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.model.domain.{NodeMapping, ObjectMapProperty}
import amf.plugins.document.vocabularies.resolution.pipelines.DialectTransformationPipeline
import amf.plugins.document.vocabularies.validation.AMFDialectValidations

import scala.concurrent.{ExecutionContext, Future}

// TODO ARM check this object
private[amf] case class DialectRegister(d: Dialect) {

  val dialect: Dialect = {
    if (d.resolved) d
    else {
      val cloned = d.cloneUnit().asInstanceOf[Dialect]
      resolveDialect(cloned)
    }
  }

  def register(amlConfig: AMLConfiguration): AMLConfiguration = {
    val existingDialects = amlConfig.registry.plugins.parsePlugins.collect {
      case plugin: AMLDialectInstanceParsingPlugin => plugin.dialect
    }
    val finder  = DefaultNodeMappableFinder(existingDialects)
    val profile = new AMFDialectValidations(dialect)(finder).profile() // TODO ARM if i use resolved dialect this throws null pointer
    val newConfig = amlConfig
      .withPlugins(plugins)
      .withValidationProfile(profile)
      .withEntities(domainModels)
      .withExtensions(dialect.extensions())
    newConfig
  }

  private lazy val plugins: List[AMFPlugin[_]] = {
    List(new AMLDialectInstanceParsingPlugin(dialect), new AMLDialectInstanceRenderingPlugin(dialect)) ++ AMLDialectNamespaceAliasesPlugin
      .forDialect(dialect)
  }

  private[amf] def resolveDialect(cloned: Dialect) = {
    TransformationPipelineRunner(DefaultErrorHandler())
      .run(cloned, DialectTransformationPipeline())
      .asInstanceOf[Dialect]
  }

  private lazy val domainModels = {
    dialect.declares
      .collect({
        case n: NodeMapping => n.id -> buildMetamodel(n) // TODO ARM wrong, for compatibilidad
      })
      .toMap
  }

  private def buildMetamodel(nodeMapping: NodeMapping): DialectDomainElementModel = {
    val nodeType = nodeMapping.nodetypeMapping
    val fields   = nodeMapping.propertiesMapping().map(_.toField)
    val mapPropertiesInDomain = dialect.declares
      .collect {
        case nodeMapping: NodeMapping =>
          nodeMapping.propertiesMapping().filter(_.classification() == ObjectMapProperty)
      }
      .flatten
      .filter(prop => prop.objectRange().exists(_.value() == nodeMapping.id))

    val mapPropertiesFields =
      mapPropertiesInDomain
        .map(_.mapTermKeyProperty())
        .distinct
        .map(iri => Field(Type.Str, ValueType(iri.value()), ModelDoc(ModelVocabularies.Parser, "custom", iri.value())))

    val nodeTypes = nodeType.option().map(Seq(_)).getOrElse(Nil)
    val result =
      new DialectDomainElementModel(nodeTypes :+ nodeMapping.id, fields ++ mapPropertiesFields, Some(nodeMapping))
    result
  }
}
