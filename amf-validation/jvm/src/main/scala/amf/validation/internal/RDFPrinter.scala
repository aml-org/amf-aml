package amf.validation.internal

import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.{Lang, RDFDataMgr, RDFFormat}
import org.apache.jena.util.FileUtils
import amf.core.internal.remote.Mimes._

import java.io.StringWriter

/**
  * Transforms a RDF Model into a JSON-LD string
  */
object RDFPrinter {

  def apply(model: Model, syntax: String): String = {
    val writer = new StringWriter()
    val format = syntax match {
      case FileUtils.langN3                                       => RDFFormat.NT
      case FileUtils.langNTriple                                  => RDFFormat.NTRIPLES
      case FileUtils.langTurtle                                   => RDFFormat.TURTLE
      case FileUtils.langXML                                      => RDFFormat.RDFXML
      case FileUtils.langXMLAbbrev                                => RDFFormat.RDFXML_ABBREV
      case "JSON-LD" | `application/ld+json` | `application/json` => RDFFormat.JSONLD_EXPAND_PRETTY
      case _                                                      => RDFFormat.TURTLE // default case
    }
    RDFDataMgr.write(writer, model, format)
    writer.toString
  }

}