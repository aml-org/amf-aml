package amf.aml.internal.utils

import amf.aml.client.scala.AMLConfiguration
import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.{NodeMapping, ObjectMapProperty, PropertyMapping}
import amf.aml.internal.metamodel.domain.DialectDomainElementModel
import amf.aml.internal.namespace.DialectNamespaceAliases
import amf.aml.internal.parse.plugin.AMLDialectInstanceParsingPlugin
import amf.aml.internal.render.emitters.instances.DefaultNodeMappableFinder
import amf.aml.internal.render.plugin.AMLDialectInstanceRenderingPlugin
import amf.aml.internal.transform.pipelines.DialectTransformationPipeline
import amf.aml.internal.utils.DialectRegister.SEMANTIC_EXTENSIONS_PROFILE
import amf.aml.internal.validate.AMFDialectValidations
import amf.core.client.common.validation.ProfileName
import amf.core.client.scala.errorhandling.DefaultErrorHandler
import amf.core.client.scala.transform.TransformationPipelineRunner
import amf.core.client.scala.vocabulary.ValueType
import amf.core.internal.metamodel.domain.{ModelDoc, ModelVocabularies}
import amf.core.internal.metamodel.{Field, Type}
import amf.core.internal.plugins.AMFPlugin
import amf.core.internal.validation.core.{SeverityMapping, ValidationProfile}

object DialectRegister {
  val SEMANTIC_EXTENSIONS_PROFILE: ProfileName = ProfileName("SEMANTIC_EXTENSIONS_PROFILE")
}

private[amf] case class DialectRegister(d: Dialect, configuration: AMLConfiguration) {

  val dialect: Dialect = {
    if (d.processingData.transformed.is(true)) d
    else {
      val cloned = d.cloneUnit().asInstanceOf[Dialect]
      resolveDialect(cloned)
    }
  }

  lazy val domainMapProperties: Seq[PropertyMapping] = dialect.declares.collect { case nodeMapping: NodeMapping =>
    nodeMapping.propertiesMapping().filter(_.classification() == ObjectMapProperty)
  }.flatten

  def register(): AMLConfiguration = {
    val existingDialects = configuration.configurationState().getDialects()
    val finder           = DefaultNodeMappableFinder(existingDialects)
    val profile          = new AMFDialectValidations(dialect)(finder).profile()
    val newConfig = configuration
      .withPlugins(plugins(profile))
      .withValidationProfile(profile)
      .withEntities(domainModels)
      .withExtensions(dialect)
      .withAliases(DialectNamespaceAliases(dialect))
    updateSemanticExtensionsProfile(newConfig, profile)
  }

  private def updateSemanticExtensionsProfile(
      config: AMLConfiguration,
      dialectProfile: ValidationProfile
  ): AMLConfiguration = {
    val validationsToPropagate = dialectProfile.validations.diff(AMFDialectValidations.staticValidations)

    val profile = config.getRegistry.getConstraintsRules.getOrElse(
      SEMANTIC_EXTENSIONS_PROFILE,
      ValidationProfile(SEMANTIC_EXTENSIONS_PROFILE, None, Seq.empty, SeverityMapping())
    )
    val nextProfile = profile.copy(
      severities = profile.severities.concat(dialectProfile.severities),
      validations = profile.validations ++ validationsToPropagate
    )
    config.withValidationProfile(nextProfile)
  }

  private def plugins(constraints: ValidationProfile): List[AMFPlugin[_]] = {
    List(
      new AMLDialectInstanceParsingPlugin(dialect, Some(constraints)),
      new AMLDialectInstanceRenderingPlugin(dialect)
    )
  }

  private[amf] def resolveDialect(cloned: Dialect) = {
    TransformationPipelineRunner(DefaultErrorHandler(), configuration)
      .run(cloned, DialectTransformationPipeline())
      .asInstanceOf[Dialect]
  }

  private lazy val domainModels = {
    dialect.declares
      .collect({ case n: NodeMapping =>
        n.id -> buildMetamodel(n)
      })
      .toMap
  }

  private def buildMetamodel(nodeMapping: NodeMapping): DialectDomainElementModel = {
    val nodeType = nodeMapping.nodetypeMapping
    val fields   = nodeMapping.propertiesMapping().map(_.toField())
    val mapPropertiesInDomain =
      domainMapProperties.filter(prop => prop.objectRange().exists(_.value() == nodeMapping.id))

    val mapPropertiesFields =
      mapPropertiesInDomain
        .map(_.mapTermKeyProperty())
        .distinct
        .map(iri => Field(Type.Str, ValueType(iri.value()), ModelDoc(ModelVocabularies.Parser, "custom", iri.value())))

    val nodeTypes = nodeType.option().map(Seq(_)).getOrElse(Nil)
    new DialectDomainElementModel(nodeTypes :+ nodeMapping.id, fields ++ mapPropertiesFields, Some(nodeMapping))
  }
}
