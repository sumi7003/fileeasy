# FE-T005 PM Acceptance Checklist

## Goal

Verify that the FileEasy admin page delivers the approved file-management scope without crossing product boundaries.

## Checks

1. Core operations
- file listing works
- search works
- preview works for supported types
- rename works
- download works
- single delete works
- batch delete works

2. Rename boundary
- only base-name editing is exposed
- extension editing is not allowed in UI
- extension changes are not implied by API integration

3. Preview boundary
- supported preview types behave as expected
- unsupported office-document types are not forced into preview
- unsupported types still support metadata display and download

4. Scope compliance
- no batch download is introduced
- no log query page or dashboard is introduced
- no monitoring or system-panel surface is introduced

5. Product-shell compliance
- admin page matches approved FileEasy visual and wireframe guidance
- admin page does not drift into legacy Xplay admin-shell patterns

6. Verification
- web build evidence is provided
- any unverified UI behaviors are explicitly listed

## Reject Conditions

- extension editing is still possible
- unsupported preview types are forced into preview
- batch download or log/dashboard surfaces appear
- admin page clearly exceeds approved FileEasy scope
