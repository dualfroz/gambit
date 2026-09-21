package gambit

object MoveGen:

  def generatePseudo(pos: Position, list: MoveList, capturesOnly: Boolean = false): Unit =
    val us = pos.sideToMove
    val them = us ^ 1
    val board = pos.board
    var sq = 0
    while sq < 128 do
      if Square.onBoard(sq) then
        val piece = board(sq)
        if piece != Piece.Empty && (piece >> 3) == us then
          Piece.typeOf(piece) match
            case Piece.Pawn   => pawnMoves(pos, sq, us, them, list, capturesOnly)
            case Piece.Knight => stepMoves(pos, sq, them, Directions.knight, list, capturesOnly)
            case Piece.King   => stepMoves(pos, sq, them, Directions.king, list, capturesOnly)
            case Piece.Bishop => slideMoves(pos, sq, them, Directions.bishop, list, capturesOnly)
            case Piece.Rook   => slideMoves(pos, sq, them, Directions.rook, list, capturesOnly)
            case Piece.Queen =>
              slideMoves(pos, sq, them, Directions.bishop, list, capturesOnly)
              slideMoves(pos, sq, them, Directions.rook, list, capturesOnly)
            case _ => ()
      sq += 1
    if !capturesOnly then castlingMoves(pos, us, them, list)

  private def pawnMoves(
      pos: Position,
      sq: Int,
      us: Int,
      them: Int,
      list: MoveList,
      capturesOnly: Boolean
  ): Unit =
    val board = pos.board
    val forward = if us == Color.White then 16 else -16
    val startRank = if us == Color.White then 1 else 6
    val promoRank = if us == Color.White then 7 else 0

    val one = sq + forward
    if Square.onBoard(one) && board(one) == Piece.Empty then
      if Square.rankOf(one) == promoRank then addPromotions(sq, one, false, list)
      else if !capturesOnly then
        list.add(Move.make(sq, one))
        if Square.rankOf(sq) == startRank then
          val two = one + forward
          if board(two) == Piece.Empty then list.add(Move.make(sq, two, double = true))

    var d = 0
    while d < 2 do
      val target = sq + forward + (if d == 0 then -1 else 1)
      if Square.onBoard(target) then
        val tp = board(target)
        if tp != Piece.Empty && (tp >> 3) == them then
          if Square.rankOf(target) == promoRank then addPromotions(sq, target, true, list)
          else list.add(Move.make(sq, target, capture = true))
        else if target == pos.epSquare && pos.epSquare >= 0 then
          list.add(Move.make(sq, target, capture = true, enPassant = true))
      d += 1

  private def addPromotions(from: Int, to: Int, capture: Boolean, list: MoveList): Unit =
    list.add(Move.make(from, to, Piece.Queen, capture = capture))
    list.add(Move.make(from, to, Piece.Rook, capture = capture))
    list.add(Move.make(from, to, Piece.Bishop, capture = capture))
    list.add(Move.make(from, to, Piece.Knight, capture = capture))

  private def stepMoves(
      pos: Position,
      sq: Int,
      them: Int,
      offsets: Array[Int],
      list: MoveList,
      capturesOnly: Boolean
  ): Unit =
    val board = pos.board
    var i = 0
    while i < offsets.length do
      val t = sq + offsets(i)
      if Square.onBoard(t) then
        val tp = board(t)
        if tp == Piece.Empty then
          if !capturesOnly then list.add(Move.make(sq, t))
        else if (tp >> 3) == them then list.add(Move.make(sq, t, capture = true))
      i += 1

  private def slideMoves(
      pos: Position,
      sq: Int,
      them: Int,
      offsets: Array[Int],
      list: MoveList,
      capturesOnly: Boolean
  ): Unit =
    val board = pos.board
    var i = 0
    while i < offsets.length do
      var t = sq + offsets(i)
      var stop = false
      while Square.onBoard(t) && !stop do
        val tp = board(t)
        if tp == Piece.Empty then
          if !capturesOnly then list.add(Move.make(sq, t))
          t += offsets(i)
        else
          if (tp >> 3) == them then list.add(Move.make(sq, t, capture = true))
          stop = true
      i += 1

  private def castlingMoves(pos: Position, us: Int, them: Int, list: MoveList): Unit =
    val board = pos.board
    if us == Color.White then
      if (pos.castling & Castling.WhiteKing) != 0 &&
        board(5) == Piece.Empty && board(6) == Piece.Empty &&
        !pos.isAttacked(4, them) && !pos.isAttacked(5, them) && !pos.isAttacked(6, them)
      then list.add(Move.make(4, 6, castle = true))
      if (pos.castling & Castling.WhiteQueen) != 0 &&
        board(1) == Piece.Empty && board(2) == Piece.Empty && board(3) == Piece.Empty &&
        !pos.isAttacked(4, them) && !pos.isAttacked(3, them) && !pos.isAttacked(2, them)
      then list.add(Move.make(4, 2, castle = true))
    else
      if (pos.castling & Castling.BlackKing) != 0 &&
        board(117) == Piece.Empty && board(118) == Piece.Empty &&
        !pos.isAttacked(116, them) && !pos.isAttacked(117, them) && !pos.isAttacked(118, them)
      then list.add(Move.make(116, 118, castle = true))
      if (pos.castling & Castling.BlackQueen) != 0 &&
        board(113) == Piece.Empty && board(114) == Piece.Empty && board(115) == Piece.Empty &&
        !pos.isAttacked(116, them) && !pos.isAttacked(115, them) && !pos.isAttacked(114, them)
      then list.add(Move.make(116, 114, castle = true))

  def generateLegal(pos: Position, list: MoveList): Unit =
    val pseudo = new MoveList()
    generatePseudo(pos, pseudo)
    val mover = pos.sideToMove
    var i = 0
    while i < pseudo.size do
      val m = pseudo(i)
      val undo = pos.make(m)
      if !pos.isAttacked(pos.kingSquare(mover), pos.sideToMove) then list.add(m)
      pos.unmake(m, undo)
      i += 1
