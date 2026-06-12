package gambit

object TT:
  val Empty = 0
  val Exact = 1
  val Lower = 2
  val Upper = 3
  val Unknown = Int.MaxValue
  val MateBound = Searcher.MateValue - 128

final class TranspositionTable(bits: Int = 20):
  private val size = 1 << bits
  private val mask = size - 1
  private val keys = new Array[Long](size)
  private val moves = new Array[Int](size)
  private val scores = new Array[Int](size)
  private val depthsArr = new Array[Byte](size)
  private val flagsArr = new Array[Byte](size)

  var hitMove: Int = 0

  def clear(): Unit =
    java.util.Arrays.fill(keys, 0L)
    java.util.Arrays.fill(moves, 0)
    java.util.Arrays.fill(flagsArr, TT.Empty.toByte)

  private inline def index(hash: Long): Int = (hash & mask).toInt

  def probeMove(hash: Long): Int =
    val idx = index(hash)
    if flagsArr(idx) != TT.Empty && keys(idx) == hash then moves(idx) else 0

  def probe(hash: Long, depth: Int, alpha: Int, beta: Int, ply: Int): Int =
    val idx = index(hash)
    if flagsArr(idx) == TT.Empty || keys(idx) != hash then
      hitMove = 0
      TT.Unknown
    else
      hitMove = moves(idx)
      if depthsArr(idx) < depth then TT.Unknown
      else
        var s = scores(idx)
        if s >= TT.MateBound then s -= ply
        else if s <= -TT.MateBound then s += ply
        flagsArr(idx) match
          case TT.Exact                     => s
          case TT.Lower if s >= beta        => s
          case TT.Upper if s <= alpha       => s
          case _                            => TT.Unknown

  def store(hash: Long, depth: Int, score: Int, flag: Int, move: Int, ply: Int): Unit =
    val idx = index(hash)
    val occupied = flagsArr(idx) != TT.Empty
    if !occupied || depth >= depthsArr(idx) then
      var s = score
      if s >= TT.MateBound then s += ply
      else if s <= -TT.MateBound then s -= ply
      keys(idx) = hash
      moves(idx) = move
      scores(idx) = s
      depthsArr(idx) = depth.toByte
      flagsArr(idx) = flag.toByte
