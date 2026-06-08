package gambit

object Bitboards:
  inline def sq64(sq: Int): Int = Square.rankOf(sq) * 8 + Square.fileOf(sq)
  inline def fileOf(s: Int): Int = s & 7
  inline def rankOf(s: Int): Int = s >> 3
  inline def bit(s: Int): Long = 1L << s
  inline def count(bb: Long): Int = java.lang.Long.bitCount(bb)

  val fileMask: Array[Long] =
    val out = new Array[Long](8)
    var f = 0
    while f < 8 do
      var r = 0
      while r < 8 do
        out(f) |= bit(r * 8 + f)
        r += 1
      f += 1
    out

  val rankMask: Array[Long] =
    val out = new Array[Long](8)
    var r = 0
    while r < 8 do
      var f = 0
      while f < 8 do
        out(r) |= bit(r * 8 + f)
        f += 1
      r += 1
    out

  val adjacentFiles: Array[Long] =
    val out = new Array[Long](8)
    var f = 0
    while f < 8 do
      if f > 0 then out(f) |= fileMask(f - 1)
      if f < 7 then out(f) |= fileMask(f + 1)
      f += 1
    out

  private val forwardRanksWhite: Array[Long] =
    val out = new Array[Long](8)
    var r = 0
    while r < 8 do
      var rr = r + 1
      while rr < 8 do
        out(r) |= rankMask(rr)
        rr += 1
      r += 1
    out

  private val forwardRanksBlack: Array[Long] =
    val out = new Array[Long](8)
    var r = 0
    while r < 8 do
      var rr = r - 1
      while rr >= 0 do
        out(r) |= rankMask(rr)
        rr -= 1
      r += 1
    out

  val passedWhite: Array[Long] =
    val out = new Array[Long](64)
    var s = 0
    while s < 64 do
      val f = fileOf(s)
      val span = fileMask(f) | adjacentFiles(f)
      out(s) = span & forwardRanksWhite(rankOf(s))
      s += 1
    out

  val passedBlack: Array[Long] =
    val out = new Array[Long](64)
    var s = 0
    while s < 64 do
      val f = fileOf(s)
      val span = fileMask(f) | adjacentFiles(f)
      out(s) = span & forwardRanksBlack(rankOf(s))
      s += 1
    out

  val frontFileWhite: Array[Long] =
    val out = new Array[Long](64)
    var s = 0
    while s < 64 do
      out(s) = fileMask(fileOf(s)) & forwardRanksWhite(rankOf(s))
      s += 1
    out

  val frontFileBlack: Array[Long] =
    val out = new Array[Long](64)
    var s = 0
    while s < 64 do
      out(s) = fileMask(fileOf(s)) & forwardRanksBlack(rankOf(s))
      s += 1
    out

  val kingRing: Array[Long] =
    val out = new Array[Long](64)
    val steps = Array((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1))
    var s = 0
    while s < 64 do
      val f = fileOf(s)
      val r = rankOf(s)
      var mask = bit(s)
      var i = 0
      while i < steps.length do
        val nf = f + steps(i)._1
        val nr = r + steps(i)._2
        if nf >= 0 && nf < 8 && nr >= 0 && nr < 8 then mask |= bit(nr * 8 + nf)
        i += 1
      out(s) = mask
      s += 1
    out

  val knightAttacks: Array[Long] =
    val out = new Array[Long](64)
    val steps = Array((1, 2), (2, 1), (2, -1), (1, -2), (-1, -2), (-2, -1), (-2, 1), (-1, 2))
    var s = 0
    while s < 64 do
      val f = fileOf(s)
      val r = rankOf(s)
      var i = 0
      while i < steps.length do
        val nf = f + steps(i)._1
        val nr = r + steps(i)._2
        if nf >= 0 && nf < 8 && nr >= 0 && nr < 8 then out(s) |= bit(nr * 8 + nf)
        i += 1
      s += 1
    out
