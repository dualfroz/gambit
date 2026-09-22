package gambit

class WinAtChessTest extends munit.FunSuite:

  test("every bundled position parses and its best move is legal and written in exact SAN") {
    for line <- WinAtChess.positions do
      val epd = Epd.parse(line)
      val pos = Fen.parse(epd.fen)
      assert(epd.bestMoves.nonEmpty, epd.id)
      for san <- epd.bestMoves do
        val move = San.parse(pos, san)
        assert(move != 0, s"${epd.id}: $san is not legal")
        assertEquals(San.toSan(pos, move), san, epd.id)
  }

  test("bundled positions carry distinct Win at Chess ids") {
    val ids = WinAtChess.positions.map(Epd.parse(_).id)
    assert(ids.forall(_.matches("WAC\\.\\d{3}")), ids.mkString(" "))
    assertEquals(ids.distinct.size, ids.size)
  }
