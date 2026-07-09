package gambit

class GameSelfPlayTest extends munit.FunSuite:

  test("self-play drives a won position into checkmate") {
    val outcome = Game.selfPlay("6k1/5ppp/8/8/8/8/8/R6K w - - 0 1", 10, 3, 0)
    assertEquals(outcome.result, "1-0 (white mates)")
    assertEquals(outcome.moves, List("a1a8"))
  }

  test("self-play recognises stalemate as a regulation draw") {
    val outcome = Game.selfPlay("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1", 10, 3, 0)
    assertEquals(outcome.result, "1/2-1/2 (stalemate)")
    assertEquals(outcome.plies, 0)
  }

  test("a self-play game serialises to a PGN that reads back to the same moves") {
    val outcome = Game.selfPlay("6k1/5ppp/8/8/8/8/8/R6K w - - 0 1", 10, 3, 0)
    val pgn = Game.pgn(outcome)
    assert(pgn.contains("Ra8#"), pgn)
    assert(pgn.trim.endsWith("1-0"), pgn)
    val reloaded = Pgn.read(pgn)
    assertEquals(reloaded.moves, outcome.moveInts)
    assertEquals(reloaded.result, "1-0")
  }

  test("result strings map to PGN result tokens") {
    assertEquals(Game.resultToken("1-0 (white mates)"), "1-0")
    assertEquals(Game.resultToken("0-1 (black mates)"), "0-1")
    assertEquals(Game.resultToken("1/2-1/2 (stalemate)"), "1/2-1/2")
    assertEquals(Game.resultToken("adjudicated draw (ply limit)"), "1/2-1/2")
  }
