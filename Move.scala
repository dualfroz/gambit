package gambit

object Move:
  private val ToShift = 7
  private val PromoShift = 14
  private val CaptureBit = 1 << 17
  private val EnPassantBit = 1 << 18
  private val DoubleBit = 1 << 19
  private val CastleBit = 1 << 20

  def make(
      from: Int,
      to: Int,
      promo: Int = 0,
      capture: Boolean = false,
      enPassant: Boolean = false,
      double: Boolean = false,
      castle: Boolean = false
  ): Int =
    from |
      (to << ToShift) |
      (promo << PromoShift) |
      (if capture then CaptureBit else 0) |
      (if enPassant then EnPassantBit else 0) |
      (if double then DoubleBit else 0) |
      (if castle then CastleBit else 0)

  inline def from(m: Int): Int = m & 0x7f
  inline def to(m: Int): Int = (m >> ToShift) & 0x7f
  inline def promo(m: Int): Int = (m >> PromoShift) & 7
  inline def isCapture(m: Int): Boolean = (m & CaptureBit) != 0
  inline def isEnPassant(m: Int): Boolean = (m & EnPassantBit) != 0
  inline def isDouble(m: Int): Boolean = (m & DoubleBit) != 0
  inline def isCastle(m: Int): Boolean = (m & CastleBit) != 0
  inline def isPromotion(m: Int): Boolean = promo(m) != 0

  def uci(m: Int): String =
    val base = Square.name(from(m)) + Square.name(to(m))
    if isPromotion(m) then base + promoChar(promo(m)) else base

  private def promoChar(t: Int): String = t match
    case Piece.Knight => "n"
    case Piece.Bishop => "b"
    case Piece.Rook   => "r"
    case Piece.Queen  => "q"
    case _            => ""

final class MoveList(capacity: Int = 256):
  private val data = new Array[Int](capacity)
  var size = 0

  def add(m: Int): Unit =
    data(size) = m
    size += 1

  def apply(i: Int): Int = data(i)

  def update(i: Int, m: Int): Unit = data(i) = m

  def clear(): Unit = size = 0
