package gambit

class PerftTest extends munit.FunSuite:
  val kiwipete = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"
  val position3 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"
  val position4 = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1"
  val position5 = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"
  val position6 = "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10"

  def perft(fen: String, depth: Int): Long = Perft.perft(Fen.parse(fen), depth)

  test("startpos perft 1") { assertEquals(perft(Fen.startPosition, 1), 20L) }
  test("startpos perft 2") { assertEquals(perft(Fen.startPosition, 2), 400L) }
  test("startpos perft 3") { assertEquals(perft(Fen.startPosition, 3), 8902L) }
  test("startpos perft 4") { assertEquals(perft(Fen.startPosition, 4), 197281L) }

  test("kiwipete perft 1") { assertEquals(perft(kiwipete, 1), 48L) }
  test("kiwipete perft 2") { assertEquals(perft(kiwipete, 2), 2039L) }
  test("kiwipete perft 3") { assertEquals(perft(kiwipete, 3), 97862L) }
  test("kiwipete perft 4") { assertEquals(perft(kiwipete, 4), 4085603L) }

  test("position3 perft 4") { assertEquals(perft(position3, 4), 43238L) }
  test("position3 perft 5") { assertEquals(perft(position3, 5), 674624L) }

  test("position4 perft 3") { assertEquals(perft(position4, 3), 9467L) }
  test("position4 perft 4") { assertEquals(perft(position4, 4), 422333L) }

  test("position5 perft 1") { assertEquals(perft(position5, 1), 44L) }
  test("position5 perft 2") { assertEquals(perft(position5, 2), 1486L) }
  test("position5 perft 3") { assertEquals(perft(position5, 3), 62379L) }
  test("position5 perft 4") { assertEquals(perft(position5, 4), 2103487L) }

  test("position6 perft 3") { assertEquals(perft(position6, 3), 89890L) }
  test("position6 perft 4") { assertEquals(perft(position6, 4), 3894594L) }
