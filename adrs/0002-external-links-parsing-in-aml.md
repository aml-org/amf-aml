# 2. External links parsing in AML

Date: 2021-01-04

## Status

Accepted

## Context

AML added an external links feature. The motivation for adding this feature can be found in the `amf-core` repo: [amf-core/adrs/0004-domainelementmodel-isexternallink-field.md](https://github.com/aml-org/amf-core/blob/master/adrs/0004-domainelementmodel-isexternallink-field.md)

The requirements for adding this feature are:
1. Add the possibility to link nodes where the URI of the target node is unknown to:
   1. The dialect instance author
   2. The dialect author, but known by the dialect instance author
2. Integrate this feature with other features in the AML spec

## Decision

Two kinds of links were created: URI links and ID template links. Moreover, a directive called `$base` was created as well.

Detailed descriptions of how these work can be found in the AML specification:
* [URI links & idTemplate links](https://github.com/aml-org/aml-spec/blob/master/dialects.md#document-nodes-linking)
* [$base](https://github.com/aml-org/aml-spec/blob/master/dialects.md#overriding-the-base-of-an-iduri)

## Consequences
URI links:
* integrate with `$id` directive feature (requirement 2)

ID template links:
* integrate with the `idTemplate` facet (requirement 2)
* support requirement 1.i: _only the keys of the ID template are known by the dialect instance author_
* support requirement 1.ii when combined with the `$base` directive: _adds the possibility of overriding the base of the 
`idTemplate` URI by the dialect instance author_
