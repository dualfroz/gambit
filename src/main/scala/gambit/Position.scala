package gambit

final class Undo(
    val captured: Int,
    val castling: Int,
    val epSquare: Int,
    val halfmove: Int,
    val hash: Long
)

final class Position:
  val board: Array[Int] = new Array[Int](128)
  var sideToMove: Int = Color.White
  var castling: Int = 0
  var epSquare: Int = -1
  var halfmove: Int = 0
  var fullmove: Int = 1
  val kingSquare: Array[Int] = Array(-1, -1)
  var hash: Long = 0L

  private inline def putPiece(sq: Int, piece: Int): Unit =
    board(sq) = piece
    hash ^= Zobrist.pieceKeys(piece)(sq)
    if (piece & 7) == Piece.King then kingSquare(piece >> 3) = sq

  private inline def removePiece(sq: Int): Unit =
    hash ^= Zobrist.pieceKeys(board(sq))(sq)
    board(sq) = Piece.Empty

  def computeHash(): Long =
    var h = 0L
    var sq = 0
    while sq < 128 do
      if Square.onBoard(sq) then
        val p = board(sq)
        if p != Piece.Empty then h ^= Zobrist.pieceKeys(p)(sq)
      sq += 1
    if sideToMove == Color.Black then h ^= Zobrist.sideKey
    h ^= Zobrist.castleKeys(castling)
    if epSquare >= 0 then h ^= Zobrist.epFileKeys(Square.fileOf(epSquare))
    h

  def isAttacked(sq: Int, by: Int): Boolean =
    val pawn = Piece.make(by, Piece.Pawn)
    if by == Color.White then
      if Square.onBoard(sq - 17) && board(sq - 17) == pawn then return true
      if Square.onBoard(sq - 15) && board(sq - 15) == pawn then return true
    else
      if Square.onBoard(sq + 17) && board(sq + 17) == pawn then return true
      if Square.onBoard(sq + 15) && board(sq + 15) == pawn then return true

    val knight = Piece.make(by, Piece.Knight)
    var i = 0
    while i < 8 do
      val c = sq + Directions.knight(i)
      if Square.onBoard(c) && board(c) == knight then return true
      i += 1

    val king = Piece.make(by, Piece.King)
    i = 0
    while i < 8 do
      val c = sq + Directions.king(i)
      if Square.onBoard(c) && board(c) == king then return true
      i += 1

    val bishop = Piece.make(by, Piece.Bishop)
    val queen = Piece.make(by, Piece.Queen)
    i = 0
    while i < 4 do
      if rayHit(sq, Directions.bishop(i), bishop, queen) then return true
      i += 1

    val rook = Piece.make(by, Piece.Rook)
    i = 0
    while i < 4 do
      if rayHit(sq, Directions.rook(i), rook, queen) then return true
      i += 1

    false

  private def rayHit(sq: Int, off: Int, sliderA: Int, sliderB: Int): Boolean =
    var c = sq + off
    while Square.onBoard(c) do
      val p = board(c)
      if p != Piece.Empty then return p == sliderA || p == sliderB
      c += off
    false

  def inCheck(color: Int): Boolean =
    isAttacked(kingSquare(color), color ^ 1)

  def make(move: Int): Undo =
    val from = Move.from(move)
    val to = Move.to(move)
    val undo = new Undo(board(to), castling, epSquare, halfmove, hash)
    val piece = board(from)
    val us = sideToMove

    if epSquare >= 0 then hash ^= Zobrist.epFileKeys(Square.fileOf(epSquare))

    removePiece(from)

    if Move.isEnPassant(move) then
      val capSq = if us == Color.White then to - 16 else to + 16
      removePiece(capSq)
    else if Move.isCapture(move) then removePiece(to)

    if Move.isPromotion(move) then putPiece(to, Piece.make(us, Move.promo(move)))
    else putPiece(to, piece)

    if Move.isCastle(move) then
      val rank = Square.rankOf(to)
      if Square.fileOf(to) == 6 then
        val rook = board(Square.make(7, rank))
        removePiece(Square.make(7, rank))
        putPiece(Square.make(5, rank), rook)
      else
        val rook = board(Square.make(0, rank))
        removePiece(Square.make(0, rank))
        putPiece(Square.make(3, rank), rook)

    val newCastling = castling & Castling.mask(from) & Castling.mask(to)
    hash ^= Zobrist.castleKeys(castling)
    hash ^= Zobrist.castleKeys(newCastling)
    castling = newCastling

    if Move.isDouble(move) then
      epSquare = if us == Color.White then from + 16 else from - 16
      hash ^= Zobrist.epFileKeys(Square.fileOf(epSquare))
    else epSquare = -1

    if (piece & 7) == Piece.Pawn || Move.isCapture(move) then halfmove = 0
    else halfmove += 1

    if us == Color.Black then fullmove += 1

    sideToMove = us ^ 1
    hash ^= Zobrist.sideKey
    undo

  def unmake(move: Int, undo: Undo): Unit =
    val from = Move.from(move)
    val to = Move.to(move)
    val us = sideToMove ^ 1

    if us == Color.Black then fullmove -= 1

    if Move.isPromotion(move) then board(from) = Piece.make(us, Piece.Pawn)
    else board(from) = board(to)
    board(to) = Piece.Empty

    if (board(from) & 7) == Piece.King then kingSquare(us) = from

    if Move.isEnPassant(move) then
      val capSq = if us == Color.White then to - 16 else to + 16
      board(capSq) = Piece.make(us ^ 1, Piece.Pawn)
    else if Move.isCapture(move) then board(to) = undo.captured

    if Move.isCastle(move) then
      val rank = Square.rankOf(to)
      if Square.fileOf(to) == 6 then
        board(Square.make(7, rank)) = board(Square.make(5, rank))
        board(Square.make(5, rank)) = Piece.Empty
      else
        board(Square.make(0, rank)) = board(Square.make(3, rank))
        board(Square.make(3, rank)) = Piece.Empty

    sideToMove = us
    castling = undo.castling
    epSquare = undo.epSquare
    halfmove = undo.halfmove
    hash = undo.hash
