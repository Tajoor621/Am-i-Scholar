# Am I Scholar — branding and screenshot integrity

## Official in-app identity
The launcher icon is a standalone vector mark combining an open book and academic cap, with a navy, white and gold palette. Android resources:
- `app/src/main/res/drawable/ic_scholar_mark.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/values/brand_colors.xml`

The vector is original app branding; the larger AI-generated promotional collage is a **concept illustration**, not a screenshot captured from a running application.

## Screenshot acceptance policy
Do not describe conceptual UI renders as real application screenshots. Before publishing store screenshots, capture the installed app with an Android emulator or device, confirm each visible feature actually exists, redact private research data and test the image at phone and tablet sizes.

## App manifest
The manifest references the standalone launcher icon and resource-backed app name, and disables cleartext traffic and automatic backup. No network activity is needed for local research tasks.

## Accuracy
This branding commit does not certify that citation standards, plagiarism checking, AI assistance, accessibility or submission workflows are fully implemented. The zero-mandatory-fee policy remains unchanged.
