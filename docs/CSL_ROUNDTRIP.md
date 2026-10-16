# Citation Center round-trip extension

The web Citation Center supports local import of a CSL-JSON array (maximum 5 MB and 2,000 records), always labeling imported metadata as unverified. The original CSL item is retained in each local research record under a `CSL-JSON:` field. Exports preserve original CSL fields (except for remapping the internal record ID) and the offline CSL formatter uses the preserved item.

Existing manually entered records continue to use the original text-field parser. This extension does not change the web storage key, Android Room tables, or the version of the JSON project backup.

## Scope of tests

Browser tests cover import preservation of non-ASCII author names and journal fields, plus basic output for all seven bundled CSL styles. These are smoke/conformance checks, **not independent official golden reference fixtures**. Verify rendered references against official style guidance and original scholarly sources.
