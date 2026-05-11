# FileEasy Validation Templates

This folder stores reusable templates for the post-validation release-closure stage.

## Current Templates

1. `FE-V104-release-blocker-fix.task-pack.template.json`
   - clone this when PM needs one bounded blocker-fix task
2. `FE-V104-release-blocker-fix.prompt.template.md`
   - use this to generate the execution prompt for that blocker-fix task
3. `FE-V104-pm-acceptance.template.md`
   - use this to run PM re-acceptance after the blocker fix returns

## Usage Rule

Do not dispatch directly from this template.

Instead:

1. identify one blocker from `FE-V102` or `FE-V103`
2. copy the template into a concrete `FE-V104-<slug>` task pack
3. narrow `allowedPaths`
4. narrow acceptance checks
5. generate a matching concrete prompt
6. accept or reject the fix with the PM acceptance template
