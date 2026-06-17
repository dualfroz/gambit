package gambit

object TimeControl:
  val defaultMovesToGo = 30
  val overheadMs = 25L
  val maxDepth = 64

  final case class Limits(
      wtime: Long = -1L,
      btime: Long = -1L,
      winc: Long = 0L,
      binc: Long = 0L,
      movestogo: Int = 0,
      movetime: Long = -1L,
      depth: Int = -1,
      infinite: Boolean = false
  ):
    def hasClock: Boolean = wtime >= 0L || btime >= 0L

  final case class Plan(depth: Int, timeMs: Long)

  def plan(limits: Limits, whiteToMove: Boolean): Plan =
    val depth = if limits.depth > 0 then math.min(limits.depth, maxDepth) else maxDepth
    val time = budgetMs(limits, whiteToMove)
    Plan(depth, time)

  def budgetMs(limits: Limits, whiteToMove: Boolean): Long =
    if limits.infinite then 0L
    else if limits.movetime > 0L then math.max(1L, limits.movetime - overheadMs)
    else if limits.hasClock then
      val myTime = if whiteToMove then limits.wtime else limits.btime
      val myInc = if whiteToMove then limits.winc else limits.binc
      if myTime <= 0L then math.max(1L, myInc - overheadMs)
      else
        val moves = if limits.movestogo > 0 then limits.movestogo else defaultMovesToGo
        val base = myTime / moves + myInc * 3L / 4L
        val hardCap = math.max(1L, (myTime * 4L) / 5L)
        val budget = math.min(base, hardCap)
        math.max(1L, budget - overheadMs)
    else 0L

  def parse(tokens: List[String]): Limits =
    var limits = Limits()
    var rest = tokens
    while rest.nonEmpty do
      rest match
        case "wtime" :: v :: tail     => limits = limits.copy(wtime = v.toLong); rest = tail
        case "btime" :: v :: tail     => limits = limits.copy(btime = v.toLong); rest = tail
        case "winc" :: v :: tail      => limits = limits.copy(winc = v.toLong); rest = tail
        case "binc" :: v :: tail      => limits = limits.copy(binc = v.toLong); rest = tail
        case "movestogo" :: v :: tail => limits = limits.copy(movestogo = v.toInt); rest = tail
        case "movetime" :: v :: tail  => limits = limits.copy(movetime = v.toLong); rest = tail
        case "depth" :: v :: tail     => limits = limits.copy(depth = v.toInt); rest = tail
        case "infinite" :: tail       => limits = limits.copy(infinite = true); rest = tail
        case _ :: tail                => rest = tail
        case Nil                      => rest = Nil
    limits
