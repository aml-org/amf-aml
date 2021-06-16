package amf.aml.client.scala.model.domain
import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.domain.DomainElement
import amf.aml.internal.metamodel.domain.{MergePolicies, MergeableMappingModel}

trait MergeableMapping extends MergeableMappingModel { this: DomainElement =>
  def mergePolicy: StrField = fields.field(MergePolicy)

  def withMergePolicy(mergePolicy: String): MergeableMapping = {
    if (MergePolicies.isAllowed(mergePolicy)) {
      set(MergePolicy, mergePolicy)
    } else {
      throw new Exception(s"Unknown merging policy: '$mergePolicy'")
    }
  }
}
