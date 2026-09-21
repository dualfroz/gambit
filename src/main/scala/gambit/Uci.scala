package gambit

final class Uci:
  private val searcher = new Searcher
  private var pos = Fen.parse(Fen.startPosition)
  private val history = scala.collection.mutable.ArrayBuffer[Long](pos.hash)
  private var worker: Thread = null

  def handle(line: String): List[String] =
    val tokens = line.trim.split("\\s+").filter(_.nonEmpty).toList
    tokens match
      case Nil => Nil
      case cmd :: rest =>
        cmd match
          case "uci"        => idLines
          case "isready"    => List("readyok")
          case "ucinewgame" => resetGame(); Nil
          case "position"   => setPosition(rest); Nil
          case "go" =>
            val collected = scala.collection.mutable.ListBuffer[String]()
            runGo(rest, collected += _)
            collected.toList
          case "stop" => searcher.stop(); Nil
          case "quit" => Nil
          case _      => Nil

  def run(input: Iterator[String], out: String => Unit): Unit =
    var quit = false
    while !quit && input.hasNext do
      val line = input.next()
      val tokens = line.trim.split("\\s+").filter(_.nonEmpty).toList
      tokens match
        case "quit" :: _ => searcher.stop(); joinWorker(); quit = true
        case "stop" :: _ => searcher.stop(); joinWorker()
        case "go" :: rest =>
          joinWorker()
          worker = new Thread(() => runGo(rest, out))
          worker.setDaemon(true)
          worker.start()
        case ("position" | "ucinewgame") :: _ =>
          joinWorker()
          handle(line).foreach(out)
        case _ => handle(line).foreach(out)

  private def idLines: List[String] =
    List("id name gambit", "id author Michal Fox", "uciok")

  private def joinWorker(): Unit =
    if worker != null then
      worker.join()
      worker = null

  private def resetGame(): Unit =
    searcher.tt.clear()
    pos = Fen.parse(Fen.startPosition)
    history.clear()
    history += pos.hash

  private def setPosition(args: List[String]): Unit =
    val rest =
      args match
        case "startpos" :: tail =>
          pos = Fen.parse(Fen.startPosition)
          tail
        case "fen" :: tail =>
          val movesIdx = tail.indexOf("moves")
          val fenTokens = if movesIdx < 0 then tail else tail.take(movesIdx)
          pos = Fen.parse(fenTokens.mkString(" "))
          if movesIdx < 0 then Nil else tail.drop(movesIdx)
        case other => other
    history.clear()
    history += pos.hash
    rest match
      case "moves" :: moveTokens => applyMoves(moveTokens)
      case _                     => ()

  private def applyMoves(moveTokens: List[String]): Unit =
    for token <- moveTokens do
      val legal = new MoveList()
      MoveGen.generateLegal(pos, legal)
      var i = 0
      var found = -1
      while i < legal.size && found < 0 do
        if Move.uci(legal(i)) == token then found = legal(i)
        i += 1
      if found >= 0 then
        pos.make(found)
        history += pos.hash

  private def runGo(args: List[String], emit: String => Unit): Unit =
    val limits = TimeControl.parse(args)
    val whiteToMove = pos.sideToMove == Color.White
    val plan = TimeControl.plan(limits, whiteToMove)
    searcher.onIteration = (d, score, nodes, ms, pv) => emit(infoLine(d, score, nodes, ms, pv))
    searcher.setGameHistory(history.toArray, history.size - 1)
    val result = searcher.search(pos, plan.depth, plan.timeMs)
    searcher.onIteration = (_, _, _, _, _) => ()
    val bestMove = if result.bestMove != 0 then Move.uci(result.bestMove) else "0000"
    emit(s"bestmove $bestMove")

  private def infoLine(depth: Int, score: Int, nodes: Long, ms: Long, pv: List[Int]): String =
    val scoreStr =
      if math.abs(score) >= TT.MateBound then
        val plies = Searcher.MateValue - math.abs(score)
        val movesToMate = (plies + 1) / 2
        s"mate ${if score > 0 then movesToMate else -movesToMate}"
      else s"cp $score"
    val nps = if ms > 0 then nodes * 1000 / ms else nodes
    val pvStr = pv.map(Move.uci).mkString(" ")
    s"info depth $depth score $scoreStr nodes $nodes nps $nps time $ms pv $pvStr"
