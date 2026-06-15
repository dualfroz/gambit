package gambit

object Draw:
  def isFiftyMove(pos: Position): Boolean = pos.halfmove >= 100

  def isInsufficientMaterial(pos: Position): Boolean =
    var minors = 0
    var sq = 0
    while sq < 128 do
      if Square.onBoard(sq) then
        Piece.typeOf(pos.board(sq)) match
          case Piece.Pawn | Piece.Rook | Piece.Queen => return false
          case Piece.Knight | Piece.Bishop           => minors += 1
          case _                                      => ()
      sq += 1
    minors <= 1

  def isThreefold(history: collection.Seq[Long], current: Long): Boolean =
    var count = 0
    var i = 0
    while i < history.length do
      if history(i) == current then count += 1
      i += 1
    count >= 3
