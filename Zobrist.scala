package gambit

object Zobrist:
  private val rng = new scala.util.Random(0x9E3779B97F4A7C15L)

  val pieceKeys: Array[Array[Long]] = Array.fill(15, 128)(rng.nextLong())
  val sideKey: Long = rng.nextLong()
  val castleKeys: Array[Long] = Array.fill(16)(rng.nextLong())
  val epFileKeys: Array[Long] = Array.fill(8)(rng.nextLong())
