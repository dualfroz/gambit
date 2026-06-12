package gambit

object Fen:
  val startPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

  def parse(fen: String): Position =
    val parts = fen.trim.split("\\s+")
    val pos = new Position
    var rank = 7
    for row <- parts(0).split("/") do
      var file = 0
      for ch <- row do
        if ch.isDigit then file += (ch - '0')
        else
          val piece = charToPiece(ch)
          val sq = Square.make(file, rank)
          pos.board(sq) = piece
          if (piece & 7) == Piece.King then pos.kingSquare(piece >> 3) = sq
          file += 1
      rank -= 1

    pos.sideToMove = if parts(1) == "w" then Color.White else Color.Black

    var castle = 0
    if parts(2) != "-" then
      for ch <- parts(2) do
        ch match
          case 'K' => castle |= Castling.WhiteKing
          case 'Q' => castle |= Castling.WhiteQueen
          case 'k' => castle |= Castling.BlackKing
          case 'q' => castle |= Castling.BlackQueen
          case _   => ()
    pos.castling = castle

    pos.epSquare = if parts(3) == "-" then -1 else Square.fromName(parts(3))
    pos.halfmove = if parts.length > 4 then parts(4).toInt else 0
    pos.fullmove = if parts.length > 5 then parts(5).toInt else 1
    pos.hash = pos.computeHash()
    pos

  def toFen(pos: Position): String =
    val sb = new StringBuilder
    var rank = 7
    while rank >= 0 do
      var empty = 0
      var file = 0
      while file < 8 do
        val p = pos.board(Square.make(file, rank))
        if p == Piece.Empty then empty += 1
        else
          if empty > 0 then
            sb.append(empty)
            empty = 0
          sb.append(pieceToChar(p))
        file += 1
      if empty > 0 then sb.append(empty)
      if rank > 0 then sb.append('/')
      rank -= 1

    sb.append(' ')
    sb.append(if pos.sideToMove == Color.White then 'w' else 'b')
    sb.append(' ')
    if pos.castling == 0 then sb.append('-')
    else
      if (pos.castling & Castling.WhiteKing) != 0 then sb.append('K')
      if (pos.castling & Castling.WhiteQueen) != 0 then sb.append('Q')
      if (pos.castling & Castling.BlackKing) != 0 then sb.append('k')
      if (pos.castling & Castling.BlackQueen) != 0 then sb.append('q')
    sb.append(' ')
    sb.append(if pos.epSquare < 0 then "-" else Square.name(pos.epSquare))
    sb.append(' ')
    sb.append(pos.halfmove)
    sb.append(' ')
    sb.append(pos.fullmove)
    sb.toString

  private def charToPiece(ch: Char): Int =
    val color = if ch.isUpper then Color.White else Color.Black
    val pieceType = ch.toLower match
      case 'p' => Piece.Pawn
      case 'n' => Piece.Knight
      case 'b' => Piece.Bishop
      case 'r' => Piece.Rook
      case 'q' => Piece.Queen
      case 'k' => Piece.King
      case _   => Piece.Empty
    Piece.make(color, pieceType)

  private def pieceToChar(piece: Int): Char =
    val letter = Piece.typeOf(piece) match
      case Piece.Pawn   => 'p'
      case Piece.Knight => 'n'
      case Piece.Bishop => 'b'
      case Piece.Rook   => 'r'
      case Piece.Queen  => 'q'
      case Piece.King   => 'k'
      case _            => '?'
    if Piece.colorOf(piece) == Color.White then letter.toUpper else letter
