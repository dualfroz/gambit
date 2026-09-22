package gambit

class MoveGenTest extends munit.FunSuite:

  private def legal(fen: String): Vector[Int] =
    val moves = new MoveList()
    MoveGen.generateLegal(Fen.parse(fen), moves)
    (0 until moves.size).map(moves.apply).toVector

  test("en passant generated in matching position") {
    val moves = legal("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3")
    val ep = moves.filter(Move.isEnPassant)
    assertEquals(ep.size, 1)
    assertEquals(Move.uci(ep.head), "e5d6")
  }

  test("both castling moves generated when legal") {
    val moves = legal("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
    val castles = moves.filter(Move.isCastle).map(Move.uci).toSet
    assertEquals(castles, Set("e1g1", "e1c1"))
  }

  test("castling blocked when king passes through check") {
    val moves = legal("r3k2r/8/8/8/8/8/6q1/R3K2R w KQkq - 0 1")
    val castles = moves.filter(Move.isCastle).map(Move.uci).toSet
    assertEquals(castles, Set("e1c1"))
  }

  test("promotions generated with all four pieces") {
    val moves = legal("8/P7/8/8/8/8/8/k6K w - - 0 1")
    val promos = moves.filter(Move.isPromotion).map(Move.uci).toSet
    assertEquals(promos, Set("a7a8q", "a7a8r", "a7a8b", "a7a8n"))
  }

  test("pinned knight cannot move") {
    val moves = legal("4r2k/8/8/8/8/8/4N3/4K3 w - - 0 1")
    val knightMoves = moves.filter(m => Move.from(m) == Square.make(4, 1))
    assertEquals(knightMoves.size, 0)
  }

  test("only check evasions when in check") {
    val moves = legal("4k3/8/8/8/8/8/8/r3K2R w K - 0 1")
    assert(moves.nonEmpty)
    for m <- moves do
      val pos = Fen.parse("4k3/8/8/8/8/8/8/r3K2R w K - 0 1")
      val undo = pos.make(m)
      assert(!pos.isAttacked(pos.kingSquare(Color.White), Color.Black), s"illegal ${Move.uci(m)}")
      pos.unmake(m, undo)
  }
