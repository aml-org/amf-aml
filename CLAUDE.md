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

### Stages to Modify
- **Publishing stages**: `Publish`

### Jenkinsfile Updates
- **For RC**: Add `"release/*"` to branch list in `Publish` stage
- **For Release**: Remove `"release/*"` from branch list in `Publish` stage

## Release Steps

### Publishing RC (x.y.z-RC.r)
1. Update build.sbt with amf-core RC version from previous step
2. From develop: `git checkout -b release/x.y.z`
3. Edit Jenkinsfile: add `"release/*"` to Publish stage branches
4. Edit build.sbt: update version to `x.y.z-RC.r`
5. Commit: `git commit -m "Publish x.y.z-RC.r"`
6. Push: `git push -u origin release/x.y.z`

### Publishing Release (x.y.z)
1. Update build.sbt with amf-core release version
2. Follow standard three-PR process (setup → master → develop)
3. After merging to master, manually tag: `git tag x.y.z origin/master && git push origin x.y.z`

### Publishing Hotfix (x.y.z-n)
1. Checkout/create `support/x.y.z` branch from tag
2. Cherry-pick required commits
3. Update version in build.sbt to `x.y.z-n`
4. Check if amf-core hotfix is also needed and update dependency
5. Ensure Jenkinsfile has `support/*` in Publish stage (for first HF only)
6. Push and create PR to merge into `support/x.y.z`

## Dependencies Impact
After releasing amf-aml, update the version in:
- amf (next in release line)
- amf-validation-report-dialect and amf-validation-profile-dialect (post-release, if snapshot changed)
