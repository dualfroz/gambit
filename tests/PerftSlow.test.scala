package gambit

class PerftSlowTest extends munit.FunSuite:
  override val munitTimeout = scala.concurrent.duration.Duration(120, "s")

  val kiwipete = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"
  val position3 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"
  val position4 = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1"
  val position5 = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"

  test("startpos perft 5") {
    assertEquals(Perft.perft(Fen.parse(Fen.startPosition), 5), 4865609L)
  }

  test("startpos perft 6") {
    assertEquals(Perft.perft(Fen.parse(Fen.startPosition), 6), 119060324L)
  }

  test("kiwipete perft 5") {
    assertEquals(Perft.perft(Fen.parse(kiwipete), 5), 193690690L)
  }

  test("position3 perft 6") {
    assertEquals(Perft.perft(Fen.parse(position3), 6), 11030083L)
  }

  test("position4 perft 5") {
    assertEquals(Perft.perft(Fen.parse(position4), 5), 15833292L)
  }

  test("position5 perft 5") {
    assertEquals(Perft.perft(Fen.parse(position5), 5), 89941194L)
  }
