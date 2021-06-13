package amf.plugins.document.vocabularies.model.domain

import amf.core.client.scala.vocabulary.ValueType

object AmlScalars {
  val all: Seq[String] = Seq("string",
                             "integer",
                             "boolean",
                             "float",
                             "decimal",
                             "double",
                             "duration",
                             "dateTime",
                             "time",
                             "date",
                             "anyUri",
                             "uri",
                             "anyType",
                             "any")
}
