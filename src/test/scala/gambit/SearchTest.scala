package gambit

class SearchTest extends munit.FunSuite:

  private def mateThreshold = Searcher.MateValue - 100

  test("finds mate in 1 for white") {
    val pos = Fen.parse("6k1/5ppp/8/8/8/8/8/R6K w - - 0 1")
    val (move, score) = new Searcher().bestMove(pos, 3)
    assertEquals(Move.uci(move), "a1a8")
    assert(score >= mateThreshold, s"expected mate score, got $score")
  }

  test("finds mate in 1 for black") {
    val pos = Fen.parse("r6k/8/8/8/8/8/5PPP/6K1 b - - 0 1")
    val (move, score) = new Searcher().bestMove(pos, 3)
    assertEquals(Move.uci(move), "a8a1")
    assert(score >= mateThreshold, s"expected mate score, got $score")
  }

  test("captures the hanging queen") {
    val pos = Fen.parse("4k3/8/8/8/8/8/3q4/4K1R1 w - - 0 1")
    val (move, _) = new Searcher().bestMove(pos, 3)
    assertEquals(Move.to(move), Square.make(3, 1))
  }

  test("evaluation is symmetric on start position") {
    assertEquals(Eval.evaluate(Fen.parse(Fen.startPosition)), 0)
  }
