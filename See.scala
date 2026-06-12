package gambit

object See:
  private val value = Array(0, 100, 320, 330, 500, 900, 20000)

  private val pawnCaptureWhite = Array(-17, -15)
  private val pawnCaptureBlack = Array(17, 15)

  def gain(pos: Position, move: Int): Int =
    val board = pos.board
    val to = Move.to(move)
    val from = Move.from(move)
    val us = pos.sideToMove
    val removed = new Array[Boolean](128)

    if Move.isEnPassant(move) then
      val capSq = if us == Color.White then to - 16 else to + 16
      removed(capSq) = true

    val victim = if Move.isEnPassant(move) then Piece.Pawn else Piece.typeOf(board(to))
    val gains = new Array[Int](40)
    gains(0) = value(victim)
    removed(from) = true
    var occupant = value(Piece.typeOf(board(from)))
    var side = us ^ 1
    var d = 0
    var running = true

    while running do
      val attacker = leastAttacker(pos, to, side, removed)
      if attacker < 0 then running = false
      else if Piece.typeOf(board(attacker)) == Piece.King && leastAttacker(pos, to, side ^ 1, removed) >= 0 then
        running = false
      else
        d += 1
        gains(d) = occupant - gains(d - 1)
        occupant = value(Piece.typeOf(board(attacker)))
        removed(attacker) = true
        side ^= 1

    while d > 0 do
      gains(d - 1) = -math.max(-gains(d - 1), gains(d))
      d -= 1
    gains(0)

  def isWinning(pos: Position, move: Int): Boolean = gain(pos, move) >= 0

  def isLosing(pos: Position, move: Int): Boolean = gain(pos, move) < 0

  private def leastAttacker(pos: Position, target: Int, side: Int, removed: Array[Boolean]): Int =
    val board = pos.board
    var bestSquare = -1
    var bestValue = Int.MaxValue

    val pawn = Piece.make(side, Piece.Pawn)
    val pawnOffsets = if side == Color.White then pawnCaptureWhite else pawnCaptureBlack
    var i = 0
    while i < 2 do
      val s = target + pawnOffsets(i)
      if Square.onBoard(s) && !removed(s) && board(s) == pawn && value(Piece.Pawn) < bestValue then
        bestValue = value(Piece.Pawn)
        bestSquare = s
      i += 1

    val knight = Piece.make(side, Piece.Knight)
    i = 0
    while i < Directions.knight.length do
      val s = target + Directions.knight(i)
      if Square.onBoard(s) && !removed(s) && board(s) == knight && value(Piece.Knight) < bestValue then
        bestValue = value(Piece.Knight)
        bestSquare = s
      i += 1

    val bishop = Piece.make(side, Piece.Bishop)
    val queen = Piece.make(side, Piece.Queen)
    i = 0
    while i < Directions.bishop.length do
      val s = raySlider(board, target, Directions.bishop(i), removed)
      if s >= 0 then
        val p = board(s)
        if p == bishop && value(Piece.Bishop) < bestValue then
          bestValue = value(Piece.Bishop); bestSquare = s
        else if p == queen && value(Piece.Queen) < bestValue then
          bestValue = value(Piece.Queen); bestSquare = s
      i += 1

    val rook = Piece.make(side, Piece.Rook)
    i = 0
    while i < Directions.rook.length do
      val s = raySlider(board, target, Directions.rook(i), removed)
      if s >= 0 then
        val p = board(s)
        if p == rook && value(Piece.Rook) < bestValue then
          bestValue = value(Piece.Rook); bestSquare = s
        else if p == queen && value(Piece.Queen) < bestValue then
          bestValue = value(Piece.Queen); bestSquare = s
      i += 1

    val king = Piece.make(side, Piece.King)
    i = 0
    while i < Directions.king.length do
      val s = target + Directions.king(i)
      if Square.onBoard(s) && !removed(s) && board(s) == king && value(Piece.King) < bestValue then
        bestValue = value(Piece.King)
        bestSquare = s
      i += 1

    bestSquare

  private def raySlider(board: Array[Int], target: Int, offset: Int, removed: Array[Boolean]): Int =
    var c = target + offset
    while Square.onBoard(c) do
      if !removed(c) && board(c) != Piece.Empty then return c
      c += offset
    -1
