package amf.aml.internal.convert

import amf.core.internal.convert.CoreClientConverters

object VocabulariesClientConverter extends VocabulariesBaseConverter with VocabulariesBaseClientConverter {
  // Overriding to match type
  override type ClientOption[E] = CoreClientConverters.ClientOption[E]
  override type ClientList[E]   = CoreClientConverters.ClientList[E]
  override type ClientFuture[T] = CoreClientConverters.ClientFuture[T]
  override type ClientMap[E]    = CoreClientConverters.ClientMap[E]
}
