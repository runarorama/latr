object Test {
  import lazified._

  @lazify val x: Int = { println("Heya!"); 10 }

  @lazifyOptimistic val y: Int = { println("Heya!"); 10 }

  @lazifyPessimistic val z: Int = { println("Yo!"); 10 }
}

