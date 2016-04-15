# lazifed

A scala macro for reasonable lazy semantics.

This library provides three annotation macros:

`@lazify`: Rewrites a `def` or `val` so that the right-hand-side is memoized. Does not synchronize or attempt to share the memo between threads.

`@lazifyOptimistic`: Rewrites a `def` or `val` to something semantically equivalent to a `lazy val`, synchronizing on the parent object to share the memo among multiple threads. Performs best if the expected contention is low.

`@lazifyPessimistic`: Rewrites a `def` or `val` to an atomic reference that uses _compare-and-set_ to share the memo among multiple threads. Performs better than the optimistic version when contention is high, at the cost of doing more work than necessary when contention is low.

Requires the [macro paradise compiler plugin](http://docs.scala-lang.org/overviews/macros/paradise.html).

