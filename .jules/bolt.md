## 2026-06-06 - Prevent redundant regex evaluation in router and parser
**Learning:** Found an anti-pattern in `DefaultRouter.java` where `Matcher.matches()` was being called to evaluate route paths, and if successful, a second `Matcher` was instantiated and evaluated again to extract path parameters. Also found uncompiled regex `\\s+` being evaluated using `String.split()` on every incoming request in `HttpParser.java`.
**Action:** Prevent redundant evaluations by reusing the successfully matched `Matcher` object and pre-compiling static regex patterns.
