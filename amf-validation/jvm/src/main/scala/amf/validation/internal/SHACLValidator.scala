package amf.validation.internal

import amf.core.client.common.validation.AmfProfile
import amf.core.client.scala.config.{
  AMFEvent,
  AMFEventListener,
  JenaLoadedModelEvent,
  ShaclFinishedEvent,
  ShaclLoadedRdfDataModelEvent,
  ShaclLoadedRdfShapesModelEvent,
  ShaclReportPrintingFinishedEvent,
  ShaclReportPrintingStartedEvent,
  ShaclValidationFinishedEvent,
  ShaclValidationStartedEvent
}
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.rdf.RdfModel
import amf.core.internal.rdf.RdfModelEmitter
import amf.core.internal.remote.Mimes._
import amf.core.internal.unsafe.PlatformSecrets
import amf.core.internal.validation.core.{ShaclValidationOptions, ValidationReport, ValidationSpecification}
import amf.validation.internal
import amf.validation.internal.emitters.ValidationRdfModelEmitter
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.shacl.{ShaclValidator, Shapes}
import org.apache.jena.util.FileUtils

import java.nio.charset.Charset
import scala.concurrent.{ExecutionContext, Future}

class SHACLValidator(listeners: Seq[AMFEventListener] = Seq.empty)
    extends amf.core.internal.validation.core.SHACLValidator
    with PlatformSecrets {

  var functionUrl: Option[String]  = None
  var functionCode: Option[String] = None

  val formats = Map(
      `application/ld+json` -> "JSON-LD",
      `application/json`    -> "JSON-LD",
      `text/n3`             -> FileUtils.langN3,
      `text/turtle`         -> FileUtils.langTurtle
  )

  private def notifyEvent(event: AMFEvent) = listeners.foreach(_.notifyEvent(event))

  override def validate(data: String, dataMediaType: String, shapes: String, shapesMediaType: String)(
      implicit executionContext: ExecutionContext): Future[String] =
    Future {
      val dataModel: Model   = loadModel(StringUtils.chomp(data), dataMediaType)
      val shapesModel: Model = loadModel(StringUtils.chomp(shapes), shapesMediaType)
      val shaclShapes        = Shapes.parse(shapesModel)
      val report             = ShaclValidator.get.validate(shaclShapes, dataModel.getGraph)
      RDFPrinter(report.getModel, "JSON-LD")
    }

  private def loadModel(data: String, mediaType: String): Model = {
    formats.get(mediaType) match {
      case Some(format) =>
        val dataModel = ModelFactory.createDefaultModel()
        dataModel.read(IOUtils.toInputStream(data, Charset.defaultCharset()), "urn:dummy", format)
        dataModel
      case None => throw new Exception(s"Unsupported media type $mediaType")
    }
  }

  override def report(data: String, dataMediaType: String, shapes: String, shapesMediaType: String)(
      implicit executionContext: ExecutionContext): Future[ValidationReport] =
    validate(data, dataMediaType, shapes, shapesMediaType).map(new JVMValidationReport(_))

  /**
    * Registers a library in the validator
    */
  override def registerLibrary(url: String, code: String): Unit = {
    functionUrl = Some(url)
    functionCode = Some(code)
  }

  override def validate(data: BaseUnit, shapes: Seq[ValidationSpecification], options: ShaclValidationOptions)(
      implicit executionContext: ExecutionContext): Future[String] =
    Future {
      val dataModel = new JenaRdfModel()
      new RdfModelEmitter(dataModel).emit(data, options.toRenderOptions)
      notifyEvent(ShaclLoadedRdfDataModelEvent(data.id, dataModel))

      val shapesModel = new JenaRdfModel()
      new ValidationRdfModelEmitter(options.messageStyle.profileName, shapesModel).emit(shapes)
      notifyEvent(ShaclLoadedRdfShapesModelEvent(data.id, shapesModel))

      val shaclShapes = Shapes.parse(shapesModel.native().asInstanceOf[Model])
      notifyEvent(JenaLoadedModelEvent(data.id))
      notifyEvent(ShaclValidationStartedEvent(data.id))
      val report = ShaclValidator.get.validate(shaclShapes, dataModel.native().asInstanceOf[Model].getGraph)
      notifyEvent(ShaclValidationFinishedEvent(data.id))

      notifyEvent(ShaclReportPrintingStartedEvent(data.id))
      val output = internal.RDFPrinter(report.getModel, "JSON-LD")
      notifyEvent(ShaclReportPrintingFinishedEvent(data.id))
      notifyEvent(ShaclFinishedEvent(data.id))
      output
    }

  override def report(data: BaseUnit, shapes: Seq[ValidationSpecification], options: ShaclValidationOptions)(
      implicit executionContext: ExecutionContext): Future[ValidationReport] =
    validate(data, shapes, options: ShaclValidationOptions).map(new JVMValidationReport(_))

  override def shapes(shapes: Seq[ValidationSpecification], functionsUrl: String): RdfModel = {
    val shapesModel = new JenaRdfModel()
    new ValidationRdfModelEmitter(AmfProfile, shapesModel).emit(shapes)
    shapesModel
  }

  override def emptyRdfModel(): RdfModel = new JenaRdfModel()

  override def supportsJSFunctions: Boolean = false
}
