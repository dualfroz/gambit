package gambit

class SanTest extends munit.FunSuite:

  private def moveByUci(fen: String, uci: String): (Position, Int) =
    val pos = Fen.parse(fen)
    val legal = new MoveList()
    MoveGen.generateLegal(pos, legal)
    var found = 0
    var i = 0
    while i < legal.size do
      if Move.uci(legal(i)) == uci then found = legal(i)
      i += 1
    (pos, found)

  private def san(fen: String, uci: String): String =
    val (pos, m) = moveByUci(fen, uci)
    San.toSan(pos, m)

  test("plain piece and pawn moves") {
    assertEquals(san(Fen.startPosition, "g1f3"), "Nf3")
    assertEquals(san(Fen.startPosition, "e2e4"), "e4")
  }

  test("captures, en passant and promotion") {
    assertEquals(san("4k3/8/8/8/6n1/8/8/4K1R1 w - - 0 1", "g1g4"), "Rxg4")
    assertEquals(san("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3", "e5d6"), "exd6")
    assertEquals(san("8/P7/8/8/6k1/8/8/7K w - - 0 1", "a7a8q"), "a8=Q")
    assertEquals(san("8/P7/8/8/6k1/8/8/7K w - - 0 1", "a7a8n"), "a8=N")
  }

  test("castling both sides") {
    assertEquals(san("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "e1g1"), "O-O")
    assertEquals(san("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "e1c1"), "O-O-O")
  }

  test("check and checkmate suffixes") {
    assertEquals(san("6k1/5ppp/8/8/8/8/8/R6K w - - 0 1", "a1a8"), "Ra8#")
    assertEquals(san("4k3/8/8/8/8/8/8/4K1R1 w - - 0 1", "g1g8"), "Rg8+")
  }

  test("file disambiguation between two knights") {
    val fen = "4k3/8/8/8/8/5N2/8/1N2K3 w - - 0 1"
    assertEquals(san(fen, "b1d2"), "Nbd2")
    assertEquals(san(fen, "f3d2"), "Nfd2")
  }

  test("rank disambiguation between two rooks on a file") {
    val fen = "4k3/8/8/8/8/R7/8/R3K3 w - - 0 1"
    assertEquals(san(fen, "a1a2"), "R1a2")
    assertEquals(san(fen, "a3a2"), "R3a2")
  }

  test("every legal move round-trips through SAN") {
    val positions = List(
      Fen.startPosition,
      "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
      "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3",
      "8/P7/8/8/8/8/8/k6K w - - 0 1",
      "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
      "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"
    )
    for fen <- positions do
      val pos = Fen.parse(fen)
      val legal = new MoveList()
      MoveGen.generateLegal(pos, legal)
      var i = 0
      while i < legal.size do
        val m = legal(i)
        val text = San.toSan(pos, m)
        assertEquals(San.parse(pos, text), m, s"$fen : $text")
        i += 1
  }
