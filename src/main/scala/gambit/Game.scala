package gambit

object Game:
  final case class Outcome(
      moves: List[String],
      result: String,
      plies: Int,
      moveInts: List[Int],
      startFen: String
  )

  def selfPlay(startFen: String, maxPlies: Int, depth: Int, timeMs: Long): Outcome =
    val pos = Fen.parse(startFen)
    val searcher = new Searcher
    val history = scala.collection.mutable.ArrayBuffer[Long](pos.hash)
    val played = scala.collection.mutable.ListBuffer[String]()
    val ints = scala.collection.mutable.ListBuffer[Int]()
    var result = "adjudicated draw (ply limit)"
    var over = false

    while !over && played.size < maxPlies do
      val legal = new MoveList()
      MoveGen.generateLegal(pos, legal)
      if legal.size == 0 then
        result =
          if pos.inCheck(pos.sideToMove) then
            if pos.sideToMove == Color.White then "0-1 (black mates)" else "1-0 (white mates)"
          else "1/2-1/2 (stalemate)"
        over = true
      else if Draw.isFiftyMove(pos) then
        result = "1/2-1/2 (fifty-move rule)"
        over = true
      else if Draw.isInsufficientMaterial(pos) then
        result = "1/2-1/2 (insufficient material)"
        over = true
      else if Draw.isThreefold(history, pos.hash) then
        result = "1/2-1/2 (threefold repetition)"
        over = true
      else
        searcher.setGameHistory(history.toArray, history.size - 1)
        val r = searcher.search(pos, depth, timeMs)
        if r.bestMove == 0 then
          result = "aborted (no move)"
          over = true
        else
          played += Move.uci(r.bestMove)
          ints += r.bestMove
          pos.make(r.bestMove)
          history += pos.hash

    Outcome(played.toList, result, played.size, ints.toList, startFen)

  def resultToken(result: String): String =
    if result.startsWith("1-0") then "1-0"
    else if result.startsWith("0-1") then "0-1"
    else if result.startsWith("1/2") || result.contains("draw") then "1/2-1/2"
    else "*"

  def pgn(outcome: Outcome): String =
    Pgn.write(outcome.startFen, outcome.moveInts, resultToken(outcome.result))
