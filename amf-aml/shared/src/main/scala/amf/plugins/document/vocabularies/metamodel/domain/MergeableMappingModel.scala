package amf.plugins.document.vocabularies.metamodel.domain
import amf.core.metamodel.Field
import amf.core.metamodel.Type.Str
import amf.core.metamodel.domain.{ModelDoc, ModelVocabularies}
import amf.core.vocabulary.Namespace

trait MergeableMappingModel {
  val MergePolicy: Field = Field(
      Str,
      Namespace.Meta + "mergePolicy",
      ModelDoc(ModelVocabularies.Meta,
               "merge policy",
               "Indication of how to merge this graph node when applying a patch document")
  )
}

object MergePolicies {
  // Nodes: identity by URI           // scalars: identity by value
  val INSERT = "insert" // add or ignore if present         // add or ignore if present
  val DELETE = "delete" // remove or ignore if no present   // remove or ignore if no present
  val UPDATE = "update" // recursive merge only if present  // replace
  val UPSERT = "upsert" // recursive merge or add           //
  val IGNORE = "ignore" // equivalent as not present        // equivalent as not present
  val FAIL   = "fail"   // fail                             // fail

  private val allowed = Set(INSERT, DELETE, UPDATE, UPSERT, IGNORE, FAIL)

  def isAllowed(policy: String): Boolean = allowed.contains(policy)
}
