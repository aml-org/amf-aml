package amf.aml.internal.parse.plugin.error

case class CannotParseDocumentException(message: String) extends Exception(s"Cannot parse document: $message")
