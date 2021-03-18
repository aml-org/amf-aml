package amf.plugins.document.vocabularies

import amf.client.environment.AMLEnvironment
import amf.client.execution.BaseExecutionEnvironment
import amf.client.parse.DefaultErrorHandler
import amf.client.remod.BaseEnvironment
import amf.client.remod.internal.FilterType
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
import amf.internal.resource.StringResourceLoader
import amf.plugins.document.vocabularies.metamodel.domain.DialectDomainElementModel
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstanceUnit}
import amf.plugins.document.vocabularies.model.domain.{DialectDomainElement, NodeMapping, ObjectMapProperty}
import amf.plugins.document.vocabularies.resolution.pipelines.DialectResolutionPipeline
import org.mulesoft.common.core._

import scala.concurrent.{ExecutionContext, Future}

class DialectsRegistry(var e: AMLEnvironment) extends AMFDomainEntityResolver with PlatformSecrets {

  protected var resolved: Set[String] = Set()

  private[vocabularies] var validations: Map[String, ValidationProfile] = Map()


  def findNode(dialectNode: String): Option[(Dialect, NodeMapping)] = {
    allDialects().find(dialect => dialectNode.contains(dialect.id)) map { dialect =>
      (dialect, dialect.declares.find(_.id == dialectNode))
    } collectFirst { case (dialect, Some(nodeMapping: NodeMapping)) => (dialect, nodeMapping) }
  }

  def knowsHeader(header: String): Boolean = findDialectForHeader(header).isDefined

  def knowsDialectInstance(instance: DialectInstanceUnit): Boolean = dialectFor(instance).isDefined

  def dialectFor(instance: DialectInstanceUnit): Option[Dialect] = {
    for {
      dialectId <- instance.definedBy().option()
      dialect   <- dialectById(dialectId)
    } yield {
      dialect
    }
  }

  def allDialects(): Iterable[Dialect] = e.registry.plugins.allPlugins.filterByType[AMLInstancePlugin].map(_.dialect)

  def register(dialect: Dialect): DialectsRegistry = {
    e = e.withDialect(dialect)
    resolved -= dialect.header
    validations -= dialect.header
    this
  }

  def findDialectForHeader(rawHeader: String): Option[Dialect] = {
    allDialects().find(_.header == headerKey(rawHeader))
  }

  def dialectById(id: String): Option[Dialect] = allDialects().find(_.id == id)

  def withRegisteredDialect(header: String)(k: Dialect => Option[DialectInstanceUnit]): Option[DialectInstanceUnit] = {
    findDialectForHeader(header) match {
      case Some(dialect) => withRegisteredDialect(dialect)(k)
      case _             => None
    }
  }

  def withRegisteredDialect(dialect: Dialect)(
      fn: Dialect => Option[DialectInstanceUnit]): Option[DialectInstanceUnit] = {
    if (!resolved.contains(dialect.header))
      fn(resolveDialect(dialect))
    else
      fn(dialect)
  }

  private def resolveDialect(dialect: Dialect) = {
    e = e.withoutDialect(dialect)
    val solved = new DialectResolutionPipeline(DefaultErrorHandler()).resolve(dialect)
    e = e.withDialect(solved)
    resolved += dialect.header
    solved
  }

  protected def headerKey(header: String): String = header.split("\\|").head.stripSpaces

  // Should not be here
  override def findType(typeString: String): Option[Obj] = {
    val foundMapping: Option[(Dialect, DomainElement)] = allDialects()
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

  // Shouldn't be here
  override def buildType(modelType: Obj): Option[Annotations => AmfObject] = modelType match {
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

  // Shouldn't be here
  def buildMetaModel(nodeMapping: NodeMapping, dialect: Dialect): DialectDomainElementModel = {
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
    new DialectDomainElementModel(nodeTypes, fields ++ mapPropertiesFields, Some(nodeMapping))
  }

  def resolveRegisteredDialect(header: String): Unit = {
    findDialectForHeader(header) match {
      case Some(dialect) => resolveDialect(dialect)
      case _             => throw new Exception(s"Cannot find Dialect with header '$header'")
    }
  }

  def registerDialect(uri: String, exec: BaseExecutionEnvironment): Future[Dialect] =
    registerDialect(uri, Environment(exec), exec)

  def registerDialect(uri: String,
                      environment: Environment = Environment(),
                      exec: BaseExecutionEnvironment = platform.defaultExecutionEnvironment): Future[Dialect] =
    registerDialect(uri, environment, exec.executionContext)

  def registerDialect(uri: String, environment: Environment, exec: ExecutionContext): Future[Dialect] = {
    implicit val executionContext: ExecutionContext = exec
    dialectById(uri) match {
      case Some(dialect) => Future.successful(dialect)
      case _ =>
        val newEnv              = AMFPluginsRegistry.obtainStaticEnv()
        val withLegacyEnvValues = BaseEnvironment.fromLegacy(newEnv, environment)
        val context = new CompilerContextBuilder(uri, platform, UnhandledParserErrorHandler).build(withLegacyEnvValues)
        RuntimeCompiler
          .forContext(context, Some("application/yaml"), Some(Aml.name))
          .map {
            case dialect: Dialect if dialect.hasValidHeader =>
              register(dialect)
              dialect
          }
    }

  }

  def unregisterDialect(uri: String): Unit = remove(uri)

  def registerDialect(url: String, code: String): Future[Dialect] = registerDialect(url, code, Environment())

  def registerDialect(url: String, code: String, exec: BaseExecutionEnvironment): Future[Dialect] =
    registerDialect(url, code, Environment(exec), exec)

  def registerDialect(url: String, code: String, exec: ExecutionContext): Future[Dialect] =
    registerDialect(url, code, Environment(exec), exec)

  def registerDialect(url: String, code: String, env: Environment): Future[Dialect] =
    registerDialect(url, env.add(StringResourceLoader(url, code)))

  def registerDialect(url: String, code: String, env: Environment, exec: BaseExecutionEnvironment): Future[Dialect] =
    registerDialect(url, env.add(StringResourceLoader(url, code)), exec)

  def registerDialect(url: String, code: String, env: Environment, exec: ExecutionContext): Future[Dialect] =
    registerDialect(url, env.add(StringResourceLoader(url, code)), exec)

  def remove(uri: String): Unit = dialectById(uri).foreach(d => e = e.withoutDialect(d))
}
