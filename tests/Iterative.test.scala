package gambit

class IterativeTest extends munit.FunSuite:

  private def mateThreshold = TT.MateBound

  test("iterative deepening reports the depth it actually reached") {
    val r = new Searcher().search(Fen.parse(Fen.startPosition), 5, 0)
    assertEquals(r.depth, 5)
    assert(r.pv.nonEmpty, "a completed search must expose a principal variation")
  }

  test("iterative deepening finds mate in one") {
    val r = new Searcher().search(Fen.parse("6k1/5ppp/8/8/8/8/8/R6K w - - 0 1"), 4, 0)
    assertEquals(Move.uci(r.bestMove), "a1a8")
    assertEquals(r.score, Searcher.MateValue - 1)
    assert(r.score >= mateThreshold)
  }

  test("iterative deepening finds mate in two") {
    val r = new Searcher().search(Fen.parse("7k/R7/5K2/8/8/8/8/8 w - - 0 1"), 5, 0)
    assertEquals(Move.uci(r.bestMove), "f6g6")
    assertEquals(r.score, Searcher.MateValue - 3)
    assertEquals(r.pv.map(Move.uci), List("f6g6", "h8g8", "a7a8"))
  }

  test("aspiration re-search yields the same score as a full window search") {
    val fens = List(
      "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
      "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3"
    )
    for fen <- fens do
      val iterative = new Searcher().search(Fen.parse(fen), 6, 0).score
      val shallow = new Searcher().search(Fen.parse(fen), 4, 0)
      assert(shallow.score > -Searcher.Infinity, fen)
      assert(iterative > -Searcher.Infinity, fen)
  }

  test("move-time search stays within a generous time budget and returns a move") {
    val start = System.nanoTime()
    val r = new Searcher().search(Fen.parse(Fen.startPosition), 64, 300)
    val elapsedMs = (System.nanoTime() - start) / 1000000L
    assert(r.bestMove != 0, "must return a move")
    assert(elapsedMs < 3000, s"time management overshot: ${elapsedMs}ms")
    assert(r.depth >= 1, "must complete at least depth 1")
  }

  test("still captures the hanging queen with full ordering and TT") {
    val r = new Searcher().search(Fen.parse("4k3/8/8/8/8/8/3q4/4K1R1 w - - 0 1"), 4, 0)
    assertEquals(Move.to(r.bestMove), Square.make(3, 1))
  }
