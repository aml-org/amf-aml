package amf.aml.internal

import amf.aml.client.scala.model.document.Dialect
import amf.core.internal.plugins.AMFPlugin

trait AMLDialectInstancePlugin[T] extends AMFPlugin[T] {
  val dialect: Dialect
  val id: String
}
