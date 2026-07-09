package gambit

class PgnTest extends munit.FunSuite:

  private def playUci(startFen: String, ucis: List[String]): List[Int] =
    val pos = Fen.parse(startFen)
    val moves = scala.collection.mutable.ListBuffer[Int]()
    for u <- ucis do
      val legal = new MoveList()
      MoveGen.generateLegal(pos, legal)
      var found = 0
      var i = 0
      while i < legal.size do
        if Move.uci(legal(i)) == u then found = legal(i)
        i += 1
      moves += found
      pos.make(found)
    moves.toList

  private val ruyLopez =
    List("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6", "b5a4", "g8f6", "e1g1", "f8e7")

  test("write produces numbered SAN movetext with a result") {
    val moves = playUci(Fen.startPosition, ruyLopez)
    val pgn = Pgn.write(Fen.startPosition, moves, "*")
    assert(pgn.contains("1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O Be7"), pgn)
    assert(pgn.contains("""[Result "*"]"""))
    assert(pgn.trim.endsWith("*"))
  }

  test("write then read reconstructs the exact move list") {
    val moves = playUci(Fen.startPosition, ruyLopez)
    val game = Pgn.read(Pgn.write(Fen.startPosition, moves, "1-0"))
    assertEquals(game.moves, moves)
    assertEquals(game.result, "1-0")
    assertEquals(game.startFen, Fen.startPosition)
  }

  test("read tolerates comments, NAGs, variations and move numbers") {
    val text =
      """[Event "Test"]
        |[Result "1-0"]
        |
        |1. e4 e5 2. Nf3 {developing} Nc6 3. Bb5 $1 a6 (3... Nf6 4. O-O) 4. Ba4 1-0
        |""".stripMargin
    val game = Pgn.read(text)
    assertEquals(game.result, "1-0")
    assertEquals(game.moves.map(m => Move.uci(m)), List("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6", "b5a4"))
    assertEquals(game.tags.find(_._1 == "Event").map(_._2), Some("Test"))
  }

  test("a game set up from a FEN keeps SetUp/FEN tags and the black-to-move number") {
    val startFen = "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3"
    val moves = playUci(startFen, List("a7a6", "b5a4", "g8f6"))
    val pgn = Pgn.write(startFen, moves, "*")
    assert(pgn.contains("""[FEN "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3"]"""), pgn)
    assert(pgn.contains("3... a6 4. Ba4 Nf6"), pgn)
    val game = Pgn.read(pgn)
    assertEquals(game.startFen, startFen)
    assertEquals(game.moves, moves)
  }
