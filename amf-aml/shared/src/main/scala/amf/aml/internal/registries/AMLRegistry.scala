package amf.aml.internal.registries

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.SemanticExtension
import amf.core.client.common.validation.ProfileName
import amf.core.client.scala.model.domain.AnnotationGraphLoader
import amf.core.client.scala.transform.TransformationPipeline
import amf.core.client.scala.validation.EffectiveValidationsCompute
import amf.core.internal.metamodel.ModelDefaultBuilder
import amf.core.internal.plugins.AMFPlugin
import amf.core.internal.plugins.parse.DomainParsingFallback
import amf.core.internal.registries.domain.EntitiesRegistry
import amf.core.internal.registries.{AMFRegistry, PluginsRegistry}
import amf.core.internal.validation.EffectiveValidations
import amf.core.internal.validation.core.ValidationProfile

/**
  * Registry to store plugins, entities, transformation pipelines, constraint rules and semantic extensions
  *
  * @param plugins                 [[PluginsRegistry]]
  * @param entitiesRegistry        [[EntitiesRegistry]]
  * @param transformationPipelines a map of [[TransformationPipeline]]s
  * @param constraintsRules        a map of [[ProfileName]] -> [[amf.core.internal.validation.core.ValidationProfile]]
  * @param extensions              a map of [[SemanticExtension]] -> [[Dialect]]
  */
private[amf] class AMLRegistry(plugins: PluginsRegistry,
                               entitiesRegistry: EntitiesRegistry,
                               transformationPipelines: Map[String, TransformationPipeline],
                               constraintsRules: Map[ProfileName, ValidationProfile],
                               effectiveValidations: Map[ProfileName, EffectiveValidations],
                               extensions: Map[String, Dialect])
    extends AMFRegistry(plugins, entitiesRegistry, transformationPipelines, constraintsRules, effectiveValidations) {

  override def withPlugin(amfPlugin: AMFPlugin[_]): AMLRegistry = copy(plugins = plugins.withPlugin(amfPlugin))

  override def removePlugin(id: String): AMLRegistry = copy(plugins = plugins.removePlugin(id))

  override def withPlugins(amfPlugins: List[AMFPlugin[_]]): AMLRegistry =
    copy(plugins = plugins.withPlugins(amfPlugins))

  override def withFallback(plugin: DomainParsingFallback): AMLRegistry = copy(plugins = plugins.withFallback(plugin))

  override def withConstraints(profile: ValidationProfile): AMLRegistry = {
    val nextRules         = constraintsRules + (profile.name -> profile)
    val computedEffective = EffectiveValidationsCompute.buildAll(nextRules)
    copy(constraintsRules = nextRules, effectiveValidations = computedEffective)
  }

  override def withConstraints(profile: ValidationProfile, effective: EffectiveValidations): AMLRegistry = {
    val nextRules     = constraintsRules + (profile.name     -> profile)
    val nextEffective = effectiveValidations + (profile.name -> effective)
    copy(constraintsRules = nextRules, effectiveValidations = nextEffective)
  }

  override def withTransformationPipeline(pipeline: TransformationPipeline): AMLRegistry =
    copy(transformationPipelines = transformationPipelines + (pipeline.name -> pipeline))

  override def withTransformationPipelines(pipelines: List[TransformationPipeline]): AMLRegistry =
    copy(transformationPipelines = transformationPipelines ++ pipelines.map(p => p.name -> p))

  override def withConstraintsRules(rules: Map[ProfileName, ValidationProfile]): AMLRegistry =
    copy(constraintsRules = constraintsRules ++ rules)

  override def withEntities(entities: Map[String, ModelDefaultBuilder]): AMLRegistry =
    copy(entitiesRegistry = entitiesRegistry.withEntities(entities))

  override def withAnnotations(annotations: Map[String, AnnotationGraphLoader]): AMLRegistry =
    copy(entitiesRegistry = entitiesRegistry.withAnnotations(annotations))

  def withExtensions(dialect: Dialect): AMLRegistry = {
    copy(extensions = this.extensions ++ dialect.extensionIndex)
      .copy(entitiesRegistry = this.entitiesRegistry.withExtensions(dialect.extensionModels))
  }

  def withExtensions(extensions: Map[String, Dialect]): AMLRegistry =
    copy(extensions = this.extensions ++ extensions)

  def getExtensionRegistry: Map[String, Dialect] = extensions

  private[amf] def findExtension(extensionName: String): Option[Dialect] = extensions.get(extensionName)

  private def copy(plugins: PluginsRegistry = plugins,
                   entitiesRegistry: EntitiesRegistry = entitiesRegistry,
                   transformationPipelines: Map[String, TransformationPipeline] = transformationPipelines,
                   constraintsRules: Map[ProfileName, ValidationProfile] = constraintsRules,
                   effectiveValidations: Map[ProfileName, EffectiveValidations] = effectiveValidations,
                   extensions: Map[String, Dialect] = extensions): AMLRegistry =
    new AMLRegistry(plugins,
                    entitiesRegistry,
                    transformationPipelines,
                    constraintsRules,
                    effectiveValidations,
                    extensions)

}

object AMLRegistry {

  /** Creates an empty AML Registry */
  val empty =
    new AMLRegistry(PluginsRegistry.empty, EntitiesRegistry.empty, Map.empty, Map.empty, Map.empty, Map.empty)

  def apply(registry: AMFRegistry): AMLRegistry =
    new AMLRegistry(
        registry.getPluginsRegistry,
        registry.getEntitiesRegistry,
        registry.getTransformationPipelines,
        registry.getConstraintsRules,
        registry.getEffectiveValidations,
        Map.empty
    )
}
