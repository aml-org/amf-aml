package amf.validation.client.platform

import amf.core.client.common.validation.AmfProfile
import amf.core.client.scala.config.{
  AMFEvent,
  AMFEventListener,
  RenderOptions,
  ShaclLoadedJsLibrariesEvent,
  ShaclLoadedRdfDataModelEvent,
  ShaclLoadedRdfShapesModelEvent,
  ShaclStartedEvent,
  ShaclValidationFinishedEvent,
  ShaclValidationStartedEvent
}
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.rdf.RdfModel
import amf.core.internal.rdf.RdfModelEmitter
import amf.core.internal.unsafe.PlatformSecrets
import amf.core.internal.validation.core.{ShaclValidationOptions, ValidationReport, ValidationSpecification}
import amf.validation.internal.RdflibRdfModel
import amf.validation.internal.emitters.ValidationRdfModelEmitter

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("SHACLValidator")
class SHACLValidator(listeners: Seq[AMFEventListener])
    extends amf.core.internal.validation.core.SHACLValidator
    with PlatformSecrets {

  var functionUrl: Option[String]  = None
  var functionCode: Option[String] = None

  private def notifyEvent(event: AMFEvent) = listeners.foreach(_.notifyEvent(event))

  def nativeShacl: js.Dynamic =
    if (js.isUndefined(js.Dynamic.global.GlobalSHACLValidator)) {
      throw new Exception("Cannot find global SHACLValidator object")
    } else {
      js.Dynamic.global.GlobalSHACLValidator
    }

  /**
    * Version of the validate function that return a JS promise instead of a Scala future
    * @param data string representation of the data graph
    * @param dataMediaType media type for the data graph
    * @param shapes string representation of the shapes graph
    * @param shapesMediaType media type fo the shapes graph
    * @return
    */
  @JSExport("validate")
  def validateJS(data: String, dataMediaType: String, shapes: String, shapesMediaType: String): js.Promise[String] = {
    // no risk in using global execution context as this is in js package.
    implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    validate(data, dataMediaType, shapes, shapesMediaType).toJSPromise
  }

  override def report(data: String, dataMediaType: String, shapes: String, shapesMediaType: String)(
      implicit executionContext: ExecutionContext): Future[ValidationReport] = {
    val promise = Promise[ValidationReport]()
    try {
      val validator = js.Dynamic.newInstance(nativeShacl)()
      loadLibrary(validator)

      val dataModel   = platform.rdfFramework.get.syntaxToRdfModel(dataMediaType, data)
      val shapesModel = platform.rdfFramework.get.syntaxToRdfModel(shapesMediaType, shapes)

      validator.validateFromModels(
          dataModel.model.native().asInstanceOf[js.Dynamic],
          shapesModel.model.native().asInstanceOf[js.Dynamic], { (e: js.Dynamic, report: js.Dynamic) =>
            if (js.isUndefined(e) || e == null) {
              val repeater: js.Array[js.Any] = js.Array()
              val result                     = new JSValidationReport(report)
              promise.success(result)
            } else {
              promise.failure(js.JavaScriptException(e))
            }
          }
      )

      promise.future
    } catch {
      case e: Exception =>
        promise.failure(e).future
    }
  }

  /**
    * Version of the report function that returns a JS promise instead of a Scala future
    * @param data string representation of the data graph
    * @param dataMediaType media type for the data graph
    * @param shapes string representation of the shapes graph
    * @param shapesMediaType media type fo the shapes graph
    * @return
    */
  @JSExport("report")
  def reportJS(data: String,
               dataMediaType: String,
               shapes: String,
               shapesMediaType: String): js.Promise[ValidationReport] = {
    // no risk in using global execution context as this is in js package.
    implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    report(data, dataMediaType, shapes, shapesMediaType).toJSPromise
  }

  /**
    * Registers a library in the validator
    *
    * @param url
    * @param code
    * @return
    */
  override def registerLibrary(url: String, code: String): Unit = {
    this.functionUrl = Some(url)
    this.functionCode = Some(code)
  }

  override def validate(data: String, dataMediaType: String, shapes: String, shapesMediaType: String)(
      implicit executionContext: ExecutionContext): Future[String] = {
    val promise   = Promise[String]()
    val validator = js.Dynamic.newInstance(nativeShacl)()
    loadLibrary(validator)
    validator.validate(
        data,
        dataMediaType,
        shapes,
        shapesMediaType, { (e: js.Dynamic, r: js.Dynamic) =>
          if (js.isUndefined(e) || e == null) {
            promise.success(js.Dynamic.global.JSON.stringify(r).toString)
          } else {
            promise.failure(js.JavaScriptException(e))
          }
        }
    )
    promise.future
  }

  protected def loadLibrary(validator: js.Dynamic): Unit = {
    if (functionCode.isDefined && functionUrl.isDefined) {
      validator.registerJSCode(functionUrl.get, functionCode.get)
    }
  }

  override def validate(data: BaseUnit, shapes: Seq[ValidationSpecification], options: ShaclValidationOptions)(
      implicit executionContext: ExecutionContext): Future[String] = {
    val promise = Promise[String]()
    try {
      notifyEvent(ShaclStartedEvent(data.id))
      val validator = js.Dynamic.newInstance(nativeShacl)()
      loadLibrary(validator)
      notifyEvent(ShaclLoadedJsLibrariesEvent(data.id))

      val dataModel = new RdflibRdfModel()
      new RdfModelEmitter(dataModel).emit(data, options.toRenderOptions)
      notifyEvent(ShaclLoadedRdfDataModelEvent(data.id, dataModel))

      val shapesModel = new RdflibRdfModel()
      new ValidationRdfModelEmitter(options.messageStyle.profileName, shapesModel).emit(shapes)
      notifyEvent(ShaclLoadedRdfShapesModelEvent(data.id, shapesModel))

      notifyEvent(ShaclValidationStartedEvent(data.id))
      validator.validateFroModels(
          dataModel.model,
          shapesModel.model, { (e: js.Dynamic, r: js.Dynamic) =>
            if (js.isUndefined(e) || e == null) {
              promise.success(js.Dynamic.global.JSON.stringify(r).toString)
            } else {
              promise.failure(js.JavaScriptException(e))
            }
            notifyEvent(ShaclValidationFinishedEvent(data.id))
          }
      )

      promise.future
    } catch {
      case e: Exception =>
        promise.failure(e).future
    }
  }

  override def report(data: BaseUnit, shapes: Seq[ValidationSpecification], options: ShaclValidationOptions)(
      implicit executionContext: ExecutionContext): Future[ValidationReport] = {
    val promise = Promise[ValidationReport]()
    try {
      notifyEvent(ShaclStartedEvent(data.id))

      val validator = js.Dynamic.newInstance(nativeShacl)()
      loadLibrary(validator)
      notifyEvent(ShaclLoadedJsLibrariesEvent(data.id))

      val dataModel = new RdflibRdfModel()
      new RdfModelEmitter(dataModel).emit(data, RenderOptions().withValidation)
      notifyEvent(ShaclLoadedRdfDataModelEvent(data.id, dataModel))

      val shapesModel = new RdflibRdfModel()
      new ValidationRdfModelEmitter(options.messageStyle.profileName, shapesModel).emit(shapes)
      notifyEvent(ShaclLoadedRdfShapesModelEvent(data.id, shapesModel))

      notifyEvent(ShaclValidationStartedEvent(data.id))
      validator.validateFromModels(
          dataModel.model,
          shapesModel.model, { (e: js.Dynamic, report: js.Dynamic) =>
            if (js.isUndefined(e) || e == null) {
              val result = new JSValidationReport(report)
              promise.success(result)
            } else {
              promise.failure(js.JavaScriptException(e))
            }
            notifyEvent(ShaclValidationFinishedEvent(data.id))
          }
      )

      promise.future
    } catch {
      case e: Exception =>
        promise.failure(e).future
    }
  }

  override def shapes(shapes: Seq[ValidationSpecification], functionsUrl: String): RdfModel = {
    val shapesModel = new RdflibRdfModel()
    new ValidationRdfModelEmitter(AmfProfile, shapesModel, functionsUrl).emit(shapes)
    shapesModel
  }

  override def emptyRdfModel(): RdfModel = new RdflibRdfModel()

  override def supportsJSFunctions: Boolean = true
}
