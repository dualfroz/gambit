package gambit

class HeuristicsTest extends munit.FunSuite:
  override val munitTimeout = scala.concurrent.duration.Duration(60, "s")

  private val mateIn3 = "8/8/6k1/1R6/8/8/8/2R4K w - - 0 1"

  private def isCheckmate(pos: Position): Boolean =
    val moves = new MoveList()
    MoveGen.generateLegal(pos, moves)
    moves.size == 0 && pos.inCheck(pos.sideToMove)

  test("search solves a forced mate in three with the full mating line") {
    val r = new Searcher().search(Fen.parse(mateIn3), 6, 0)
    assertEquals(r.score, Searcher.MateValue - 5)
    assertEquals(Move.uci(r.bestMove), "c1c6")
    assertEquals(r.pv.size, 5, r.pv.map(Move.uci).mkString(" "))
    val pos = Fen.parse(mateIn3)
    for m <- r.pv do pos.make(m)
    assert(isCheckmate(pos), "the principal variation must end in checkmate")
  }

  test("check extensions expose the mate before the nominal horizon") {
    val r = new Searcher().search(Fen.parse(mateIn3), 4, 0)
    assert(r.score >= TT.MateBound, s"depth 4 should already see the mate, got ${r.score}")
  }

  test("null-move pruning does not change the search result") {
    val positions = List(
      "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
      "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
      "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3"
    )
    for fen <- positions do
      val on = new Searcher; on.useNullMove = true
      val off = new Searcher; off.useNullMove = false
      assertEquals(on.search(Fen.parse(fen), 5, 0).score, off.search(Fen.parse(fen), 5, 0).score, fen)
  }

  test("null-move pruning cuts the node count on a middlegame position") {
    val kiwi = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"
    val on = new Searcher; on.useNullMove = true
    val off = new Searcher; off.useNullMove = false
    val nOn = on.search(Fen.parse(kiwi), 6, 0).nodes
    val nOff = off.search(Fen.parse(kiwi), 6, 0).nodes
    assert(nOn < nOff, s"expected null move to prune, got $nOn vs $nOff")
  }

  test("an external stop aborts an otherwise unbounded search and still yields a move") {
    val searcher = new Searcher()
    @volatile var result: SearchResult = null
    val worker = new Thread(() => result = searcher.search(Fen.parse(Fen.startPosition), 64, 0))
    worker.start()
    Thread.sleep(150)
    searcher.stop()
    worker.join(5000)
    assert(!worker.isAlive, "search must observe the stop flag and return")
    assert(result != null && result.bestMove != 0, "a stopped search still returns its best move so far")
  }
