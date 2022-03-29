package amf.aml.client.scala.model.domain

import amf.aml.internal.metamodel.domain.AnyMappingModel._
import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.parser.domain.Fields

/* TODO This should inherit from NodeMappable and be a class (a simple allof or oneOf should be of this class).
 * but the meta field of NodeMappable make impossible to have hierarchy of classes currently.
 * Thats why this is abstract.
 */
abstract class AnyMapping(fields: Fields) extends DomainElement {

  def and: Seq[StrField]        = fields.field(And)
  def or: Seq[StrField]         = fields.field(Or)
  def components: Seq[StrField] = fields.field(Components)

  def withAnd(andMapping: Seq[String]): AnyMapping        = set(And, andMapping)
  def withOr(orMapping: Seq[String]): AnyMapping          = set(Or, orMapping)
  def withComponents(components: Seq[String]): AnyMapping = set(Components, components)

}
