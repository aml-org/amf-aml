package amf.rdf.internal.convert

import amf.core.internal.convert.{BidirectionalMatcher, CoreBaseConverter}
import amf.rdf.client.scala.{Literal, Node, PropertyObject, RdfModel, Uri}
import amf.rdf.client.platform

trait RdfBaseConverter
    extends CoreBaseConverter
    with PropertyObjectConverter
    with LiteralConverter
    with UriConverter
    with NodeConverter
    with RdfModelConverter

trait PropertyObjectConverter {
  implicit object PropertyObjectMatcher extends BidirectionalMatcher[PropertyObject, platform.PropertyObject] {
    override def asClient(from: PropertyObject): platform.PropertyObject   = new platform.PropertyObject(from)
    override def asInternal(from: platform.PropertyObject): PropertyObject = from.internal
  }
}

trait LiteralConverter {
  implicit object LiteralMatcher extends BidirectionalMatcher[Literal, platform.Literal] {
    override def asClient(from: Literal): platform.Literal   = platform.Literal(from)
    override def asInternal(from: platform.Literal): Literal = from.internal
  }
}

trait UriConverter {
  implicit object UriMatcher extends BidirectionalMatcher[Uri, platform.Uri] {
    override def asClient(from: Uri): platform.Uri   = platform.Uri(from)
    override def asInternal(from: platform.Uri): Uri = from.internal
  }
}

trait NodeConverter {
  implicit object NodeMatcher extends BidirectionalMatcher[Node, platform.Node] {
    override def asClient(from: Node): platform.Node   = platform.Node(from)
    override def asInternal(from: platform.Node): Node = from.internal
  }
}

trait RdfModelConverter {
  implicit object RdfModelMatcher extends BidirectionalMatcher[RdfModel, platform.RdfModel] {
    override def asClient(from: RdfModel): platform.RdfModel   = new platform.RdfModel(from)
    override def asInternal(from: platform.RdfModel): RdfModel = from.internal
  }
}
