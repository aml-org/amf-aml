package amf.rdf.internal.convert

import amf.core.internal.convert.CoreClientConverters

object RdfClientConverter extends RdfBaseConverter with RdfBaseClientConverter {

  override type ClientOption[E] = CoreClientConverters.ClientOption[E]
  override type ClientList[E]   = CoreClientConverters.ClientList[E]
  override type ClientFuture[T] = CoreClientConverters.ClientFuture[T]
  override type ClientMap[E]    = CoreClientConverters.ClientMap[E]
}
