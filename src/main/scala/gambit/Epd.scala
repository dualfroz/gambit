package gambit

final case class EpdPosition(
    fen: String,
    bestMoves: List[String],
    avoidMoves: List[String],
    id: String,
    operations: Map[String, String]
)

final case class EpdResult(
    position: EpdPosition,
    solved: Boolean,
    playedSan: String,
    depth: Int,
    nodes: Long,
    timeMs: Long
)

object Epd:
  def parse(line: String): EpdPosition =
    val trimmed = line.trim
    val fields = trimmed.split("\\s+")
    val board = fields.take(4).mkString(" ")
    val rest = trimmed.split("\\s+", 5)
    val opsText = if rest.length >= 5 then rest(4) else ""
    val operations = parseOperations(opsText)
    val fen = s"$board ${operations.getOrElse("hmvc", "0")} ${operations.getOrElse("fmvn", "1")}"
    EpdPosition(
      fen = fen,
      bestMoves = tokenizeMoves(operations.getOrElse("bm", "")),
      avoidMoves = tokenizeMoves(operations.getOrElse("am", "")),
      id = operations.getOrElse("id", "").replace("\"", ""),
      operations = operations
    )

  private def tokenizeMoves(operand: String): List[String] =
    operand.split("\\s+").filter(_.nonEmpty).toList

  private def parseOperations(text: String): Map[String, String] =
    val out = scala.collection.mutable.LinkedHashMap[String, String]()
    for chunk <- text.split(";") do
      val piece = chunk.trim
      if piece.nonEmpty then
        val space = piece.indexWhere(_.isWhitespace)
        if space < 0 then out(piece) = ""
        else out(piece.take(space)) = piece.drop(space).trim
    out.toMap

  def solve(position: EpdPosition, timeMs: Long, maxDepth: Int = 64): EpdResult =
    val pos = Fen.parse(position.fen)
    val good = resolve(pos, position.bestMoves)
    val bad = resolve(pos, position.avoidMoves)
    val searcher = new Searcher
    val result = searcher.search(pos, maxDepth, timeMs)
    val best = result.bestMove
    val solved =
      if position.bestMoves.nonEmpty then good.contains(best)
      else if position.avoidMoves.nonEmpty then !bad.contains(best)
      else false
    val playedSan = if best != 0 then San.toSan(pos, best) else "(none)"
    EpdResult(position, solved, playedSan, result.depth, result.nodes, timeMs)

  private def resolve(pos: Position, sans: List[String]): Set[Int] =
    sans.map(San.parse(pos, _)).filter(_ != 0).toSet

  def runSuite(lines: Seq[String], timeMs: Long, maxDepth: Int = 64): List[EpdResult] =
    lines.filter(_.trim.nonEmpty).map(line => solve(parse(line), timeMs, maxDepth)).toList

  def report(results: List[EpdResult]): String =
    val solved = results.count(_.solved)
    val sb = new StringBuilder
    for r <- results do
      val flag = if r.solved then "ok  " else "MISS"
      val label = if r.position.id.nonEmpty then r.position.id else r.position.fen
      val target = if r.position.bestMoves.nonEmpty then s"bm ${r.position.bestMoves.mkString(" ")}"
      else s"am ${r.position.avoidMoves.mkString(" ")}"
      sb.append(f"[$flag] $label%-12s played ${r.playedSan}%-8s ($target) depth ${r.depth} nodes ${r.nodes}\n")
    sb.append(s"solved ${solved}/${results.length}\n")
    sb.toString
