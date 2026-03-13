# amf-aml

**Repository**: https://github.com/aml-org/amf-aml

## Release Process
Follows the regular release process (see ~/mulesoft/CLAUDE.md).

## Release Position
- **Second** in the RC/release line
- Depends on **amf-core** RC/release
- RC and full version must be adopted in **amf** before its release

## Files & Locations

### Version File
- `build.sbt`

### Dependencies
- amf-core (update to RC version for RC, release version for release)

## Jenkins Configuration

### Dashboard
- **Base URL**: https://jenkins.build.msap.io/job/application/job/AMF/job/amf-aml
- **Branch URL format**: `{base-url}/job/{branch-name}` (URL encode `/` as `%252F`)
- **Example**: https://jenkins.build.msap.io/job/application/job/AMF/job/amf-aml/job/release%252F6.10.1

### Stages to Modify
- **Publishing stages**: `Publish`

### Jenkinsfile Updates
- **For RC**: Add `"release/*"` to branch list in `Publish` stage
- **For Release**: Remove `"release/*"` from branch list in `Publish` stage

## Dependencies Impact
After releasing amf-aml, update the version in:
- amf (next in release line)
- amf-validation-report-dialect and amf-validation-profile-dialect (post-release, if snapshot changed)
