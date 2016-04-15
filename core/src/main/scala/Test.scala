object Test {
  @lazify val x: Int = { println("Heya!"); 10 }
}
