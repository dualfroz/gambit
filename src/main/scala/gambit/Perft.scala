package gambit

object Perft:
  def perft(pos: Position, depth: Int): Long =
    if depth == 0 then 1L
    else
      val moves = new MoveList()
      MoveGen.generateLegal(pos, moves)
      if depth == 1 then moves.size.toLong
      else
        var nodes = 0L
        var i = 0
        while i < moves.size do
          val m = moves(i)
          val undo = pos.make(m)
          nodes += perft(pos, depth - 1)
          pos.unmake(m, undo)
          i += 1
        nodes

  def divide(pos: Position, depth: Int): List[(String, Long)] =
    val moves = new MoveList()
    MoveGen.generateLegal(pos, moves)
    val out = List.newBuilder[(String, Long)]
    var i = 0
    while i < moves.size do
      val m = moves(i)
      val undo = pos.make(m)
      val count = if depth <= 1 then 1L else perft(pos, depth - 1)
      pos.unmake(m, undo)
      out += (Move.uci(m) -> count)
      i += 1
    out.result()
