---
name: web-research
description: Search the web and fetch specific pages to answer current, source-dependent, price-related, or URL-based questions. Use for anything that depends on up-to-date external information.
---

# Web Research

Use this skill when the user asks for current, external, source-dependent, price-related, policy-related, API-related, version-related, news-related, or citation-required information.

## Available Tools

- `web_search`: Search the public web and return candidate pages. This is your primary tool.
- The Linux sandbox shell: use `curl -sL <url>` (optionally piped through a text extractor you install, e.g. `pip install trafilatura` then `trafilatura -u <url>`) to fetch and read a specific page when a search snippet is not enough.

## When to Use

Use this skill when:
- The user asks for latest, current, today, recent, price, availability, policy, regulation, changelog, release date, news, ranking, or comparison.
- The answer depends on external sources.
- The information may have changed since training.
- The user asks for sources, citations, or URLs.

Do not use this skill when:
- The task is translation, rewriting, formatting, or brainstorming.
- The answer is stable common knowledge.
- The user explicitly says not to search.

## Procedure

1. Decide whether web access is actually required.
2. Generate 1 to 3 focused search queries.
3. Call `web_search` with the strongest query first.
4. Prefer official, primary, recent, and authoritative sources.
5. For each important source, fetch the page with `curl -sL <url>` in the shell and read the real content — do not answer from snippets alone for time-sensitive or factual claims.
6. Extract: title, source, publication/update date, key claims, relevant evidence.
7. Filter low-quality results: duplicated pages, SEO spam, undated pages for time-sensitive claims, copied content without an original source.
8. Cross-check important claims against at least two sources when possible.

## Source Priority

1. Official documentation, official website, government source, company announcement.
2. Standards, papers, GitHub repositories, package registries.
3. Reputable media or industry publications.
4. Blogs, forums, social posts.

## Stop Conditions

Stop searching when the answer is supported by reliable sources, additional results repeat, you have checked 2–3 strong sources, or 2–3 rounds have not improved confidence.

## Output Rules

- Answer first.
- Include sources (URLs).
- Mention dates when time matters.
- If sources conflict, explain the conflict.
- If reliable information is not found, say so clearly.

_Adapted for POSH from the FoneClaw open skill set (MIT)._
