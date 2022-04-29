package amf.rdf.internal

import amf.core.client.scala.model.document.{DeclaresModel, EncodesModel}
import amf.core.internal.metamodel.document.{BaseUnitModel, DocumentModel}
import amf.core.internal.metamodel.{ModelDefaultBuilder, Obj}
import amf.core.internal.parser.ParseConfiguration
import amf.core.internal.validation.CoreValidations.UnableToParseNode
import amf.rdf.client.scala.Node
import org.mulesoft.common.core.CachedFunction
import org.mulesoft.common.functional.MonadInstances.optionMonad

class EntitiesFacade private[amf] (parserConfig: ParseConfiguration) {

  private val sorter = new DefaultNodeClassSorter()

  private def isUnitModel(typeModel: Obj): Boolean =
    typeModel.isInstanceOf[DocumentModel] || typeModel.isInstanceOf[EncodesModel] || typeModel
      .isInstanceOf[DeclaresModel] || typeModel.isInstanceOf[BaseUnitModel]

  def retrieveType(
      id: String,
      node: Node,
      findBaseUnit: Boolean = false,
      visitedSelfEncoded: Boolean = false
  ): Option[ModelDefaultBuilder] = {
    val types = sorter.sortedClassesOf(node)

    val foundType = types.find { t =>
      val maybeFoundType = findType(t)
      // this is just for self-encoding documents
      maybeFoundType match {
        case Some(typeModel) if !findBaseUnit && !isUnitModel(typeModel) => true
        case Some(typeModel) if findBaseUnit && isUnitModel(typeModel)   => true
        case _                                                           => false
      }
    } orElse {
      // if I cannot find it, I will return the matching one directly, this is used
      // in situations where the references a reified, for example, in the canonical web api spec
      types.find(findType(_).isDefined && !visitedSelfEncoded)
    }

    foundType match {
      case Some(t) => findType(t)
      case None =>
        parserConfig.eh.violation(UnableToParseNode, id, s"Error parsing JSON-LD node, unknown @types $types")
        None
    }
  }

  private val findType = CachedFunction.fromMonadic(parserConfig.registryContext.findType)

  private def findType(`type`: String): Option[ModelDefaultBuilder] = findType.runCached(`type`)

}
