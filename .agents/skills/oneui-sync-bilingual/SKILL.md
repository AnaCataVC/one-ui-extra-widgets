---
name: oneui-sync-bilingual
description: Audits and synchronizes English and Spanish strings, dynamic LocaleHelper formatting, and bilingual documentation while enforcing repository formatting rules.
tools:
  - view_file
  - grep_search
  - find_by_name
  - list_dir
  - write_to_file
  - replace_file_content
  - multi_replace_file_content
---
# Skill: Synchronize Bilingual Resources & Documentation

When invoked, act as a **Bilingual Localization & Documentation Specialist (EN / ES)** for `one-ui-extra-widgets`.

Your objective is to ensure 100% parity between English and Spanish across Android XML string resources, dynamic Kotlin strings, and Markdown documentation.

---

## Synchronization Scope & Invariants

1. **Android String Resource Parity:**
   - Compare every `<string name="...">` key in `app/src/main/res/values/strings.xml` against `app/src/main/res/values-es/strings.xml`.
   - Ensure no keys are missing in either file, placeholder specifiers (`%1$s`, `%1$d`, etc.) match in order and type, and XML special characters (such as apostrophes `\'`) are properly escaped.
2. **Dynamic Kotlin Localization (`LocaleHelper`):**
   - Inspect widgets (`app/src/main/java/com/anacatavc/oneuiextrawidgets/widgets/`), companion screens (`ui/screens/`), and domain engines (`data/`) for hardcoded user-facing strings.
   - Ensure dynamic strings use `LocaleHelper.isSpanishActive(context)` or resource lookups with both English and Spanish branches.
3. **Bilingual README & Per-Widget Documentation:**
   - Keep `README.md` (English) and `README.es.md` (Spanish) structurally identical and synchronized with all widget capabilities, permissions, and architecture notes.
   - Ensure every widget category has a dedicated documentation file in `docs/` linked from both READMEs.
4. **Formatting Invariants:**
   - Never include country flag emojis in `README.md`, `README.es.md`, `docs/`, or string resources.
   - Never include absolute local filesystem paths in documentation.

---

## Step-by-Step Execution Workflow

1. **Audit Resource Keys:**
   - Read `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml`.
   - List any missing keys, untranslated literals, or mismatched format specifiers.
2. **Audit Codebase Strings:**
   - Search Kotlin sources under `app/src/main/java/` for user-facing labels in Glance `Text(...)` and Compose `Text(...)` calls.
   - Verify each string resolves through `strings.xml` / `strings-es.xml` or `LocaleHelper.isSpanishActive()`.
3. **Audit Documentation:**
   - Compare sections and tables between `README.md` and `README.es.md`.
   - Scan `README.md`, `README.es.md`, and `docs/*.md` for prohibited country flag emojis or absolute file paths.
4. **Apply Synchronized Updates:**
   - Edit the XML, Kotlin, and Markdown files to restore full bilingual parity and report all synchronized keys and sections to the user.
