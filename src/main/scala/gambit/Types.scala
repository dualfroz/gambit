package gambit

object Color:
  val White = 0
  val Black = 1
  inline def opposite(c: Int): Int = c ^ 1

object Piece:
  val Empty = 0
  val Pawn = 1
  val Knight = 2
  val Bishop = 3
  val Rook = 4
  val Queen = 5
  val King = 6

  inline def make(color: Int, pieceType: Int): Int = (color << 3) | pieceType
  inline def typeOf(piece: Int): Int = piece & 7
  inline def colorOf(piece: Int): Int = piece >> 3

  val WhitePawn = make(Color.White, Pawn)
  val WhiteKnight = make(Color.White, Knight)
  val WhiteBishop = make(Color.White, Bishop)
  val WhiteRook = make(Color.White, Rook)
  val WhiteQueen = make(Color.White, Queen)
  val WhiteKing = make(Color.White, King)
  val BlackPawn = make(Color.Black, Pawn)
  val BlackKnight = make(Color.Black, Knight)
  val BlackBishop = make(Color.Black, Bishop)
  val BlackRook = make(Color.Black, Rook)
  val BlackQueen = make(Color.Black, Queen)
  val BlackKing = make(Color.Black, King)

object Square:
  inline def make(file: Int, rank: Int): Int = rank * 16 + file
  inline def fileOf(sq: Int): Int = sq & 7
  inline def rankOf(sq: Int): Int = sq >> 4
  inline def onBoard(sq: Int): Boolean = (sq & 0x88) == 0

  def name(sq: Int): String =
    val file = ('a' + fileOf(sq)).toChar
    val rank = ('1' + rankOf(sq)).toChar
    s"$file$rank"

  def fromName(s: String): Int = make(s(0) - 'a', s(1) - '1')

object Castling:
  val WhiteKing = 1
  val WhiteQueen = 2
  val BlackKing = 4
  val BlackQueen = 8

  val mask: Array[Int] = Array.fill(128)(15)
  mask(Square.make(4, 0)) = 15 & ~(WhiteKing | WhiteQueen)
  mask(Square.make(7, 0)) = 15 & ~WhiteKing
  mask(Square.make(0, 0)) = 15 & ~WhiteQueen
  mask(Square.make(4, 7)) = 15 & ~(BlackKing | BlackQueen)
  mask(Square.make(7, 7)) = 15 & ~BlackKing
  mask(Square.make(0, 7)) = 15 & ~BlackQueen

object Directions:
  val knight: Array[Int] = Array(33, 31, 18, 14, -14, -18, -31, -33)
  val bishop: Array[Int] = Array(17, 15, -15, -17)
  val rook: Array[Int] = Array(16, 1, -1, -16)
  val king: Array[Int] = Array(17, 16, 15, 1, -1, -15, -16, -17)
