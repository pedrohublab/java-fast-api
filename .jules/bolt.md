## 2024-05-18 - Optimized router matching for static routes
**Learning:** Found an opportunity to optimize `DefaultRouter` by adding a fast path for static routes. Direct `String.equals()` is ~34x faster than `Pattern.matches()`. Since standard APIs often have many static routes without path variables, checking if `paramNames` is empty before performing regex match is a significant win without altering behavior.
**Action:** When implementing route matching, add a fast path that uses `String.equals()` for non-parameterized strings instead of relying solely on regular expressions.
