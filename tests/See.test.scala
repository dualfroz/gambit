package gambit

class SeeTest extends munit.FunSuite:

  private def seeOf(fen: String, uci: String): Int =
    val pos = Fen.parse(fen)
    val legal = new MoveList()
    MoveGen.generateLegal(pos, legal)
    var move = 0
    var i = 0
    while i < legal.size do
      if Move.uci(legal(i)) == uci then move = legal(i)
      i += 1
    assert(move != 0, s"move $uci not legal in $fen")
    See.gain(pos, move)

  test("rook grabs an undefended pawn for a clean pawn") {
    assertEquals(seeOf("4k3/8/8/4p3/8/8/8/4R1K1 w - - 0 1", "e1e5"), 100)
  }

  test("rook takes a pawn defended by a pawn and loses the exchange") {
    assertEquals(seeOf("4k3/8/3p4/4p3/8/8/8/4R1K1 w - - 0 1", "e1e5"), -400)
  }

  test("pawn trade supported on both sides is exactly even") {
    assertEquals(seeOf("4k3/8/3p4/4p3/3P4/8/8/4K3 w - - 0 1", "d4e5"), 0)
  }

  test("knight takes a pawn defended by a pawn") {
    assertEquals(seeOf("4k3/8/3p4/4p3/8/5N2/8/4K3 w - - 0 1", "f3e5"), -220)
  }

  test("doubled rooks x-ray through the front rook to win the exchange back") {
    assertEquals(seeOf("4rk2/8/8/4p3/8/4R3/8/4R1K1 w - - 0 1", "e3e5"), 100)
  }

  test("capturing a hanging queen wins its full value") {
    assertEquals(seeOf("4k3/8/8/3q4/8/8/8/3RK3 w - - 0 1", "d1d5"), 900)
  }

  test("en passant capture is evaluated as a pawn trade") {
    assertEquals(seeOf("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", "e5d6"), 100)
  }

  test("isWinning and isLosing classify captures consistently with the swap value") {
    val fen = "4k3/8/3p4/4p3/8/5N2/8/4K3 w - - 0 1"
    assert(See.isLosing(Fen.parse(fen), findMove(fen, "f3e5")))
    val fenWin = "4k3/8/8/3q4/8/8/8/3RK3 w - - 0 1"
    assert(See.isWinning(Fen.parse(fenWin), findMove(fenWin, "d1d5")))
  }

  private def findMove(fen: String, uci: String): Int =
    val pos = Fen.parse(fen)
    val legal = new MoveList()
    MoveGen.generateLegal(pos, legal)
    var move = 0
    var i = 0
    while i < legal.size do
      if Move.uci(legal(i)) == uci then move = legal(i)
      i += 1
    move
