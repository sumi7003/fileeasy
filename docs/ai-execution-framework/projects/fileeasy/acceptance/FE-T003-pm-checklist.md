# FE-T003 PM Acceptance Checklist

## Goal

Verify that the FileEasy backend upload core is strong enough to support v1 resumable upload requirements without crossing product scope.

## Checks

1. Size limit
- backend explicitly rejects files larger than `4GB`
- rejection path and returned error are described
- file-size handling uses `Long` or equivalent safe type

2. Allowlist
- upload initialization checks the approved file whitelist
- unsupported file types are rejected with a clear error

3. Upload session model
- upload session persists more than a count
- `uploadedChunkIndexes` or equivalent persisted structure exists
- model can support out-of-order and repeated chunks

4. Recovery API
- status endpoint can return:
  - `totalChunks`
  - `uploadedChunkIndexes`
  - `missingChunkIndexes`
  - `status`

5. Completion path
- merge is blocked if chunks are missing
- merged size is checked against declared file size
- final metadata is created only after successful completion

6. Same-name handling
- same-name upload does not overwrite old files
- same-name upload does not hard-fail on name conflict
- final persisted file name is auto-renamed safely

7. Verification
- compile evidence is provided
- focused backend verification is provided where feasible
- any unverified areas are explicitly listed

## Reject Conditions

- No reliable persisted chunk index model
- No enforced `4GB` limit at init
- Same-name upload can overwrite old files
- Unsupported file types still enter the upload pipeline
