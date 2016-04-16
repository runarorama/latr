# lazifed

A scala macro for reasonable lazy semantics.

This library provides three annotation macros:

`@lazify`: Rewrites a `def` or `val` so that the right-hand-side is memoized. Does not synchronize or attempt to share the memo between threads.

`@lazifyPessimistic`: Rewrites a `def` or `val` to something semantically equivalent to a `lazy val`, synchronizing on the parent object to share the memo among multiple threads. Performs best if the expected contention is low.

`@lazifyOptimistic`: Rewrites a `def` or `val` to an atomic reference that uses _compare-and-set_ to share the memo among multiple threads. Performs better than the optimistic version when contention is high, at the cost of doing more work than necessary when contention is low.

The implementations of `lazifyOptimistic` and `lazifyPessimistic` are taken from [SIP-20](http://docs.scala-lang.org/sips/pending/improved-lazy-val-initialization.html). The "pessimistic" version is SIP-20 *Version V2*, which uses two `synchronized` blocks using `this` as a monitor. The "optimistic" version is taken from SIP-20 *Version V4*, and initializes a fresh `AtomicReference` and uses `compareAndSet` with replay to avoid locking.

## Setup

This library requires the [macro paradise compiler plugin](http://docs.scala-lang.org/overviews/macros/paradise.html). Follow the instructions there to set up the plugin.

If you're using SBT, you can get Lazified from Bintray: 

``` scala
resolvers += Resolver.bintrayRepo("runarorama", "maven")

libraryDependencies += "com.higher-order" %% "lazified" % "0.1.1"
```

## Usage

Wherever you would otherwise say something like this in Scala:

``` scala
lazy val x: Int = {
  println("I will only print once and block the world while doing it!")
  10
}
```

You can instead say:

``` scala
import lazified._

@lazify val x: Int = {
  println("I may print multiple times under thread contention, but I'm super cheap!")
  10
}
```

Or:

``` scala
@lazifyOptimistic val x: Int = {
  println("I'm totally thread-safe, but kind of expensive!")
  10
}
```

Or:

``` scala
@lazifyPessimistic val x: Int = {
  println("I will block the world, but only slightly!")
  10
}
```

