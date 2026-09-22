package gambit

class MakeUnmakeTest extends munit.FunSuite:
  val positions = List(
    Fen.startPosition,
    "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
    "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3",
    "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1"
  )

  private def snapshot(pos: Position): (Vector[Int], Int, Int, Int, Int, Int, Int, Int, Long) =
    (pos.board.toVector, pos.sideToMove, pos.castling, pos.epSquare, pos.halfmove,
      pos.fullmove, pos.kingSquare(0), pos.kingSquare(1), pos.hash)

  private def walk(pos: Position, depth: Int): Unit =
    if depth > 0 then
      val moves = new MoveList()
      MoveGen.generateLegal(pos, moves)
      var i = 0
      while i < moves.size do
        val m = moves(i)
        val before = snapshot(pos)
        val undo = pos.make(m)
        assertEquals(pos.hash, pos.computeHash(), s"incremental hash drift after ${Move.uci(m)}")
        walk(pos, depth - 1)
        pos.unmake(m, undo)
        assertEquals(snapshot(pos), before, s"state not restored after ${Move.uci(m)}")
        i += 1

  test("make/unmake restores exact state to depth 3") {
    for fen <- positions do walk(Fen.parse(fen), 3)
  }
