package gambit

class FenTest extends munit.FunSuite:
  val cases = List(
    Fen.startPosition,
    "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
    "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3",
    "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
    "8/P7/8/8/8/8/8/k6K w - - 5 42"
  )

  test("fen round trip") {
    for fen <- cases do assertEquals(Fen.toFen(Fen.parse(fen)), fen)
  }

  test("startpos hash matches recompute") {
    val pos = Fen.parse(Fen.startPosition)
    assertEquals(pos.hash, pos.computeHash())
  }
