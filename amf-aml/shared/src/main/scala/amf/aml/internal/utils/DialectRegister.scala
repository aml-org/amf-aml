package amf.aml.internal.utils

import amf.aml.client.scala.AMLConfiguration
import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.{NodeMapping, ObjectMapProperty}
import amf.aml.internal.metamodel.domain.DialectDomainElementModel
import amf.aml.internal.namespace.AMLDialectNamespaceAliasesPlugin
import amf.aml.internal.parse.plugin.AMLDialectInstanceParsingPlugin
import amf.aml.internal.render.emitters.instances.DefaultNodeMappableFinder
import amf.aml.internal.render.plugin.AMLDialectInstanceRenderingPlugin
import amf.aml.internal.transform.pipelines.DialectTransformationPipeline
import amf.aml.internal.validate.AMFDialectValidations
import amf.core.client.scala.errorhandling.DefaultErrorHandler
import amf.core.client.scala.transform.TransformationPipelineRunner
import amf.core.client.scala.vocabulary.ValueType
import amf.core.internal.metamodel.domain.{ModelDoc, ModelVocabularies}
import amf.core.internal.metamodel.{Field, Type}
import amf.core.internal.plugins.AMFPlugin

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
