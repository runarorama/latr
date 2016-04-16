object Test {
  import lazified._

  @lazify val x: Int = { println("Heya!"); 10 }

  @lazifyOptimistic val y: Int = { println("Heya!"); 10 }

  @lazifyPessimistic val z: Int = { println("Yo!"); 10 }

  object A {
    @lazify val a0: Int = B.b
    @lazify val a1: Int = 17
  }

  object B {
    @lazify val b:Int = A.a1
  }

  @lazify val foo: Int = { println("once!"); Thread.sleep(1000); 10 }
  @lazifyPessimistic val bar: Int = { println("once!"); Thread.sleep(1000); 10 }
  @lazifyOptimistic  val baz: Int = { println("once!"); Thread.sleep(1000); 10 }

  @lazify val foos: Unit = Range(1, 100).foreach { _ =>
    new Thread(new Runnable { def run = println(foo) }).start
  }

  @lazify val bars: Unit = Range(1, 100).foreach { _ =>
    new Thread(new Runnable { def run = println(bar) }).start
  }

  @lazify val bazs: Unit = Range(1, 100).foreach { _ =>
    new Thread(new Runnable { def run = println(baz) }).start
  }
}
