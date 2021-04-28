package amf.plugins.document.vocabularies.model.document

package object kind {

  sealed trait AMLDocumentKind

  // ? -- Higher hierarchy kinds
  sealed trait DialectInstanceDocumentKind extends AMLDocumentKind

  sealed trait DialectDocumentKind extends AMLDocumentKind

  // ? -- Dialect instance documents
  case object DialectInstanceFragment extends DialectInstanceDocumentKind

  case object DialectInstanceLibrary extends DialectInstanceDocumentKind

  case object DialectInstancePatch extends DialectInstanceDocumentKind

  case object DialectInstance extends DialectInstanceDocumentKind

  // ? -- Dialect documents
  case object DialectFragment extends DialectDocumentKind

  case object DialectLibrary extends DialectDocumentKind

  case object Dialect extends DialectDocumentKind

  // ? -- Vocabulary
  case object Vocabulary extends AMLDocumentKind

}
