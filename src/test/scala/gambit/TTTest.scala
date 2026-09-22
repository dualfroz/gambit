package gambit

class TTTest extends munit.FunSuite:

  test("store then probe returns exact score") {
    val tt = new TranspositionTable(16)
    tt.store(0x1234L, 5, 123, TT.Exact, 42, 0)
    assertEquals(tt.probe(0x1234L, 5, -1000, 1000, 0), 123)
    assertEquals(tt.probeMove(0x1234L), 42)
  }

  test("lower bound only usable at or above beta") {
    val tt = new TranspositionTable(16)
    tt.store(0x55L, 5, 200, TT.Lower, 7, 0)
    assertEquals(tt.probe(0x55L, 5, -1000, 150, 0), 200)
    assertEquals(tt.probe(0x55L, 5, -1000, 300, 0), TT.Unknown)
  }

  test("upper bound only usable at or below alpha") {
    val tt = new TranspositionTable(16)
    tt.store(0x66L, 5, 50, TT.Upper, 8, 0)
    assertEquals(tt.probe(0x66L, 5, 100, 1000, 0), 50)
    assertEquals(tt.probe(0x66L, 5, 0, 1000, 0), TT.Unknown)
  }

  test("shallow entry is not usable for deeper probe") {
    val tt = new TranspositionTable(16)
    tt.store(0x77L, 3, 10, TT.Exact, 0, 0)
    assertEquals(tt.probe(0x77L, 6, -1000, 1000, 0), TT.Unknown)
  }

  test("depth preferred replacement keeps the deeper entry") {
    val tt = new TranspositionTable(16)
    tt.store(0x88L, 8, 300, TT.Exact, 1, 0)
    tt.store(0x88L, 4, 999, TT.Exact, 2, 0)
    assertEquals(tt.probe(0x88L, 8, -1000, 1000, 0), 300)
  }

  test("mate score is stored relative to root distance") {
    val tt = new TranspositionTable(16)
    val mateAtPly4 = Searcher.MateValue - 4
    tt.store(0x99L, 5, mateAtPly4, TT.Exact, 0, 4)
    assertEquals(tt.probe(0x99L, 5, -Searcher.Infinity, Searcher.Infinity, 4), mateAtPly4)
    assertEquals(tt.probe(0x99L, 5, -Searcher.Infinity, Searcher.Infinity, 2), Searcher.MateValue - 2)
  }

  private val positions = List(
    Fen.startPosition,
    "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
    "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
    "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3"
  )

  private def fullWidthSearcher(useTt: Boolean): Searcher =
    val searcher = new Searcher
    searcher.useTt = useTt
    searcher.useNullMove = false
    searcher.useLmr = false
    searcher.usePruning = false
    searcher

  test("transposition table does not change the score of a full-width search") {
    for fen <- positions do
      assertEquals(
        fullWidthSearcher(useTt = true).search(Fen.parse(fen), 5, 0).score,
        fullWidthSearcher(useTt = false).search(Fen.parse(fen), 5, 0).score,
        fen
      )
  }

  test("transposition table reduces the node count") {
    for fen <- positions do
      val withTt = fullWidthSearcher(useTt = true)
      val noTt = fullWidthSearcher(useTt = false)
      val ttNodes = withTt.search(Fen.parse(fen), 5, 0).nodes
      val plainNodes = noTt.search(Fen.parse(fen), 5, 0).nodes
      assert(ttNodes < plainNodes, s"$fen: expected TT to prune, got $ttNodes vs $plainNodes")
  }
