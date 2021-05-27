package amf.client.remod

import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.plugins.document.vocabularies.model.document.Dialect

trait AMLDialectInstancePlugin[T] extends AMFPlugin[T] {
  val dialect: Dialect
  val id: String
}