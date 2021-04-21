package amf.plugins.document.vocabularies

import amf.client.parse.DefaultErrorHandler
import amf.client.remod.{AMFEnvironment, AMLDialectInstancePlugin}
import amf.core.CompilerContextBuilder
import amf.core.metamodel.domain.{ModelDoc, ModelVocabularies}
import amf.core.metamodel.{Field, Obj, Type}
import amf.core.model.domain.{AmfObject, DomainElement}
import amf.core.parser.Annotations
import amf.core.parser.errorhandler.UnhandledParserErrorHandler
import amf.core.registries.{AMFDomainEntityResolver, AMFPluginsRegistry}
import amf.core.remote.Aml
import amf.core.services.RuntimeCompiler
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.core.ValidationProfile
import amf.core.vocabulary.ValueType
import amf.internal.environment.Environment
import amf.plugins.document.vocabularies.metamodel.domain.DialectDomainElementModel
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstanceUnit}
import amf.plugins.document.vocabularies.model.domain.{DialectDomainElement, NodeMapping, ObjectMapProperty}
import amf.plugins.document.vocabularies.plugin.headers.ExtensionHeader.{
  DialectFragmentHeader,
  DialectHeader,
  DialectLibraryHeader,
  VocabularyHeader
}
import amf.plugins.document.vocabularies.resolution.pipelines.DialectResolutionPipeline
import org.mulesoft.common.collections.FilterType
import org.mulesoft.common.core._
import org.mulesoft.common.functional.MonadInstances._

import scala.concurrent.ExecutionContext

class DialectsRegistry extends AMFDomainEntityResolver with PlatformSecrets with DialectRegistration {
  type NodeMappingId = String

  private[vocabularies] var validations: Map[String, ValidationProfile] = Map()

  // Private methods
  private[amf] def env(): AMFEnvironment = AMFPluginsRegistry.staticEnvironment

  private[amf] def setEnv(env: AMFEnvironment): Unit = AMFPluginsRegistry.staticEnvironment = env

  private[amf] def resolveDialect(dialect: Dialect) =
    new DialectResolutionPipeline(DefaultErrorHandler()).resolve(dialect)

  private[amf] def invalidateCaches(): Unit = {
    findType.invalidateCache()
    buildType.invalidateCache()
    metamodelCache.invalidate()
  }
  private[amf] def instancePlugins =
    env().registry.plugins.allPlugins.toStream.filterType[AMLDialectInstancePlugin[_]]

  private[amf] def parseDialect(uri: String, environment: Environment)(implicit e: ExecutionContext) = {
    val context =
      new CompilerContextBuilder(uri, platform, UnhandledParserErrorHandler).withEnvironment(environment).build()
    RuntimeCompiler
      .forContext(context, Some("application/yaml"), Some(Aml.name))
      .map {
        case dialect: Dialect if dialect.hasValidHeader => dialect
      }
  }

  // Caches
  private val findType = CachedFunction.fromMonadic { typeString: String =>
    val foundMapping: Option[(Dialect, DomainElement)] = instancePlugins
      .map(_.dialect)
      .collect {
        case dialect: Dialect =>
          dialect.declares.find {
            case nodeMapping: NodeMapping => nodeMapping.id == typeString
            case _                        => false
          } map { nodeMapping =>
            (dialect, nodeMapping)
          }
      }
      .collectFirst { case Some(x) => x }

    foundMapping match {
      case Some((dialect: Dialect, nodeMapping: NodeMapping)) =>
        Some(buildMetaModel(nodeMapping, dialect))
      case _ => None
    }
  }
  private val buildType = CachedFunction.fromMonadic { modelType: Obj =>
    modelType match {
      case dialectModel: DialectDomainElementModel =>
        val reviver = (annotations: Annotations) =>
          dialectModel.nodeMapping match {
            case Some(nodeMapping) =>
              DialectDomainElement(annotations)
                .withInstanceTypes(dialectModel.typeIri :+ nodeMapping.id)
                .withDefinedBy(nodeMapping)
            case _ =>
              throw new Exception(s"Cannot find node mapping for dialectModel $dialectModel")
        }

        Some(reviver)
      case _ => None
    }
  }
  private val metamodelCache = new Cache[NodeMappingId, DialectDomainElementModel]

  def findNode(dialectNode: String): Option[(Dialect, NodeMapping)] = {
    instancePlugins
      .find(plugin => dialectNode.contains(plugin.dialect.id))
      .map { plugin =>
        (plugin.dialect, plugin.dialect.declares.find(_.id == dialectNode))
      }
      .collectFirst {
        case (dialect, Some(nodeMapping: NodeMapping)) => (dialect, nodeMapping)
      }
  }

  def knowsHeader(header: String): Boolean = header.stripSpaces match {
    case VocabularyHeader | DialectHeader | DialectLibraryHeader | DialectFragmentHeader => true
    case instanceHeader                                                                  => findDialectForHeader(instanceHeader).isDefined
  }

  def knowsDialectInstance(instance: DialectInstanceUnit): Boolean = dialectFor(instance).isDefined

  def dialectFor(instance: DialectInstanceUnit): Option[Dialect] =
    instance.definedBy().option().flatMap(id => instancePlugins.find(p => p.dialect.id == id).map(_.dialect))

  def allDialects(): Iterable[Dialect] = instancePlugins.map(_.dialect).distinct

  def findDialectForHeader(header: String): Option[Dialect] =
    instancePlugins.find(plugin => plugin.dialect.acceptsHeader(header)).map(_.dialect)

  def dialectById(id: String): Option[Dialect] = instancePlugins.find(p => p.dialect.id == id).map(_.dialect)

  def withRegisteredDialect(dialect: Dialect)(fn: Dialect => DialectInstanceUnit): DialectInstanceUnit = {
    if (!dialect.resolved)
      fn(resolveDialect(dialect))
    else
      fn(dialect)
  }

  override def findType(typeString: String): Option[Obj] = findType.runCached(typeString)

  override def buildType(modelType: Obj): Option[Annotations => AmfObject] = buildType.runCached(modelType)

  def buildMetaModel(nodeMapping: NodeMapping, dialect: Dialect): DialectDomainElementModel = {
    metamodelCache
      .get(nodeMapping.id)
      .getOrElse {
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
            .map(iri =>
              Field(Type.Str, ValueType(iri.value()), ModelDoc(ModelVocabularies.Parser, "custom", iri.value())))

        val nodeTypes = nodeType.option().map(Seq(_)).getOrElse(Nil)
        val result    = new DialectDomainElementModel(nodeTypes, fields ++ mapPropertiesFields, Some(nodeMapping))
        metamodelCache.put(nodeMapping.id, result)
        result
      }
  }

  def unregisterDialect(uri: String): Unit = {
    for {
      plugin <- instancePlugins.find(_.dialect.location().contains(uri))
    } yield {
      validations -= plugin.dialect.header
      setEnv {
        env().removePlugin(plugin.id)
      }
      invalidateCaches()
    }
  }

  def remove(uri: String): Unit = unregisterDialect(uri)

  def reset(): Unit = {
    setEnv {
      instancePlugins.foldLeft(env()) { (env, p) =>
        env.removePlugin(p.id)
      }
    }
    validations = Map()
    invalidateCaches()
  }
}
