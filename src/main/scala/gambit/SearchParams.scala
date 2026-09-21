package gambit

object SearchParams:
  val reverseFutilityDepth = 6
  val reverseFutilityMargin = 80

  val razorDepth = 3
  val razorMargin: Array[Int] = Array(0, 240, 300, 520)

  val futilityDepth = 6
  val futilityMargin = 90

  val lmpDepth = 6
  val lmpBase: Array[Array[Int]] =
    val out = Array.ofDim[Int](2, lmpDepth + 1)
    var d = 0
    while d <= lmpDepth do
      out(0)(d) = (3 + d * d) / 2
      out(1)(d) = 3 + d * d
      d += 1
    out

  val lmrMinIndex = 3
  val lmrMinDepth = 3
  val lmrTable: Array[Array[Int]] =
    val maxDepth = 64
    val maxMoves = 64
    val out = Array.ofDim[Int](maxDepth + 1, maxMoves + 1)
    var d = 1
    while d <= maxDepth do
      var m = 1
      while m <= maxMoves do
        val r = 0.75 + math.log(d) * math.log(m) / 2.25
        out(d)(m) = math.max(0, r.toInt)
        m += 1
      d += 1
    out

  def lmrReduction(depth: Int, moveIndex: Int): Int =
    val d = math.min(depth, 64)
    val m = math.min(moveIndex, 64)
    lmrTable(d)(m)

  val deltaMargin = 120
