package gambit

final case class SearchResult(bestMove: Int, score: Int, depth: Int, nodes: Long, pv: List[Int])

final class Searcher:
  import Searcher.*

  val tt = new TranspositionTable()
  var nodes: Long = 0L
  var useTt: Boolean = true
  var useNullMove: Boolean = true
  var onIteration: (Int, Int, Long, Long, List[Int]) => Unit = (_, _, _, _, _) => ()

  private val maxPly = 128
  private val killers = Array.ofDim[Int](maxPly, 2)
  private val historyTable = Array.ofDim[Int](2, 128 * 128)
  private val pathHashes = new Array[Long](4096)
  private val staticEvals = new Array[Int](maxPly + 4)
  private var historyLen = 0
  var useLmr: Boolean = true
  var usePruning: Boolean = true

  private var deadline = 0L
  private var useTime = false
  @volatile private var stopped = false
  private var startNanos = 0L

  def stop(): Unit = stopped = true

  def setGameHistory(hashes: Array[Long], len: Int): Unit =
    var i = 0
    while i < len do
      pathHashes(i) = hashes(i)
      i += 1
    historyLen = len

  def wouldRepeat(pos: Position, ancestors: Array[Long]): Boolean =
    setGameHistory(ancestors, ancestors.length)
    pathHashes(historyLen) = pos.hash
    isRepetition(pos, 0)

  def search(pos: Position, maxDepth: Int, timeMs: Long): SearchResult =
    newSearch()
    useTime = timeMs > 0
    startNanos = System.nanoTime()
    deadline = startNanos + timeMs * 1000000L
    val depthCap = math.min(maxDepth, maxPly - 1)

    var bestMove = 0
    var bestScore = 0
    var pv: List[Int] = Nil
    var completed = 0
    var window = AspirationWindow
    var depth = 1
    while depth <= depthCap && !stopped do
      val useAspiration = depth >= 4
      val alpha0 = if useAspiration then bestScore - window else -Infinity
      val beta0 = if useAspiration then bestScore + window else Infinity
      var (score, move) = searchRoot(pos, depth, alpha0, beta0)
      if !stopped && (score <= alpha0 || score >= beta0) then
        val res = searchRoot(pos, depth, -Infinity, Infinity)
        score = res._1
        move = res._2
      if !stopped then
        bestMove = move
        bestScore = score
        completed = depth
        pv = extractPv(pos, depth)
        onIteration(depth, bestScore, nodes, elapsedMs(), pv)
      depth += 1
    SearchResult(bestMove, bestScore, completed, nodes, pv)

  def bestMove(pos: Position, depth: Int): (Int, Int) =
    historyLen = 0
    val r = search(pos, depth, 0)
    (r.bestMove, r.score)

  private def newSearch(): Unit =
    nodes = 0L
    stopped = false
    var p = 0
    while p < maxPly do
      killers(p)(0) = 0
      killers(p)(1) = 0
      p += 1
    java.util.Arrays.fill(historyTable(0), 0)
    java.util.Arrays.fill(historyTable(1), 0)

  private def elapsedMs(): Long = (System.nanoTime() - startNanos) / 1000000L

  private def searchRoot(pos: Position, depth: Int, alpha0: Int, beta: Int): (Int, Int) =
    pathHashes(historyLen) = pos.hash
    val moves = new MoveList()
    MoveGen.generateLegal(pos, moves)
    val ttMove = if useTt then tt.probeMove(pos.hash) else 0
    orderMoves(pos, moves, ttMove, 0)

    var alpha = alpha0
    var best = -Infinity
    var bestMove = if moves.size > 0 then moves(0) else 0
    var i = 0
    while i < moves.size && !stopped do
      val m = moves(i)
      val undo = pos.make(m)
      var score =
        if i == 0 then -negamax(pos, depth - 1, -beta, -alpha, 1)
        else
          val trial = -negamax(pos, depth - 1, -alpha - 1, -alpha, 1)
          if !stopped && trial > alpha && trial < beta then -negamax(pos, depth - 1, -beta, -alpha, 1)
          else trial
      pos.unmake(m, undo)
      if !stopped then
        if score > best then
          best = score
          bestMove = m
        if best > alpha then alpha = best
        if alpha >= beta then i = moves.size
      i += 1

    if !stopped && useTt then
      val flag = if best <= alpha0 then TT.Upper else if best >= beta then TT.Lower else TT.Exact
      tt.store(pos.hash, depth, best, flag, bestMove, 0)
    (best, bestMove)

  private def negamax(pos: Position, depth0: Int, alpha0: Int, beta: Int, ply: Int): Int =
    nodes += 1
    if useTime && (nodes & 2047L) == 0L && System.nanoTime() >= deadline then stopped = true
    if stopped then return 0

    pathHashes(historyLen + ply) = pos.hash
    if isRepetition(pos, ply) || Draw.isFiftyMove(pos) then return DrawScore

    val pvNode = beta - alpha0 > 1
    val side = pos.sideToMove
    val inChk = pos.inCheck(side)
    val depth = if inChk && ply < maxPly - 1 then depth0 + 1 else depth0

    val ttScore = if useTt then tt.probe(pos.hash, depth, alpha0, beta, ply) else TT.Unknown
    val ttMove = if useTt then tt.hitMove else 0
    if ttScore != TT.Unknown && !pvNode then return ttScore

    if depth <= 0 then return quiescence(pos, alpha0, beta, ply)

    val notMateBound = beta < TT.MateBound && beta > -TT.MateBound
    val staticEval = if inChk then -Infinity else Eval.evaluate(pos)
    staticEvals(ply) = staticEval
    val improving = !inChk && ply >= 2 && staticEval > staticEvals(ply - 2)

    if usePruning && !pvNode && !inChk && notMateBound then
      if depth <= SearchParams.reverseFutilityDepth
        && staticEval - SearchParams.reverseFutilityMargin * depth >= beta
      then return staticEval - SearchParams.reverseFutilityMargin * depth

      if depth <= SearchParams.razorDepth
        && staticEval + SearchParams.razorMargin(depth) <= alpha0
      then
        val razorScore = quiescence(pos, alpha0, alpha0 + 1, ply)
        if razorScore <= alpha0 then return razorScore

    if useNullMove && !pvNode && !inChk && depth >= 3 && notMateBound
      && staticEval >= beta && hasNonPawnMaterial(pos, side)
    then
      val reduction = 2 + depth / 6
      val nullState = makeNull(pos)
      val nullScore = -negamax(pos, depth - 1 - reduction, -beta, -beta + 1, ply + 1)
      unmakeNull(pos, nullState)
      if stopped then return 0
      if nullScore >= beta then return beta

    val moves = new MoveList()
    MoveGen.generateLegal(pos, moves)
    if moves.size == 0 then
      if inChk then return -MateValue + ply else return 0

    orderMoves(pos, moves, ttMove, ply)
    val futilityBase = staticEval + SearchParams.futilityMargin * depth
    var alpha = alpha0
    var best = -Infinity
    var bestMove = 0
    var cutoff = false
    var searched = 0
    var i = 0
    while i < moves.size && !cutoff do
      val m = moves(i)
      val quiet = !Move.isCapture(m) && !Move.isPromotion(m)
      val undo = pos.make(m)
      val givesCheck = pos.inCheck(pos.sideToMove)

      val prune =
        usePruning && !pvNode && !inChk && quiet && !givesCheck && searched > 0 && best > -TT.MateBound &&
          ((depth <= SearchParams.lmpDepth && i >= SearchParams.lmpBase(if improving then 1 else 0)(depth))
            || (depth <= SearchParams.futilityDepth && futilityBase <= alpha))

      if prune then pos.unmake(m, undo)
      else
        val reduction =
          if useLmr && depth >= SearchParams.lmrMinDepth && searched >= SearchParams.lmrMinIndex
            && quiet && !inChk && !givesCheck
          then
            var r = SearchParams.lmrReduction(depth, searched)
            if !pvNode then r += 1
            if m == killers(if ply < maxPly then ply else maxPly - 1)(0) then r -= 1
            math.max(0, math.min(r, depth - 2))
          else 0

        val score =
          if searched == 0 then -negamax(pos, depth - 1, -beta, -alpha, ply + 1)
          else
            var trial = -negamax(pos, depth - 1 - reduction, -alpha - 1, -alpha, ply + 1)
            if !stopped && reduction > 0 && trial > alpha then
              trial = -negamax(pos, depth - 1, -alpha - 1, -alpha, ply + 1)
            if !stopped && trial > alpha && trial < beta then
              trial = -negamax(pos, depth - 1, -beta, -alpha, ply + 1)
            trial
        pos.unmake(m, undo)
        searched += 1
        if stopped then return 0
        if score > best then
          best = score
          bestMove = m
        if best > alpha then alpha = best
        if alpha >= beta then
          cutoff = true
          if quiet then
            recordKiller(ply, m)
            recordHistory(side, m, depth)
      i += 1

    if searched == 0 then
      if inChk then return -MateValue + ply else return DrawScore

    if useTt then
      val flag = if best <= alpha0 then TT.Upper else if cutoff then TT.Lower else TT.Exact
      tt.store(pos.hash, depth, best, flag, bestMove, ply)
    best

  private def makeNull(pos: Position): (Int, Long) =
    val savedEp = pos.epSquare
    val savedHash = pos.hash
    if savedEp >= 0 then pos.hash ^= Zobrist.epFileKeys(Square.fileOf(savedEp))
    pos.epSquare = -1
    pos.sideToMove = pos.sideToMove ^ 1
    pos.hash ^= Zobrist.sideKey
    (savedEp, savedHash)

  private def unmakeNull(pos: Position, state: (Int, Long)): Unit =
    pos.sideToMove = pos.sideToMove ^ 1
    pos.epSquare = state._1
    pos.hash = state._2

  private def hasNonPawnMaterial(pos: Position, color: Int): Boolean =
    val board = pos.board
    var sq = 0
    while sq < 128 do
      if Square.onBoard(sq) then
        val p = board(sq)
        if p != Piece.Empty && (p >> 3) == color then
          val pt = p & 7
          if pt == Piece.Knight || pt == Piece.Bishop || pt == Piece.Rook || pt == Piece.Queen then
            return true
      sq += 1
    false

  private def quiescence(pos: Position, alpha0: Int, beta: Int): Int =
    nodes += 1
    val standPat = Eval.evaluate(pos)
    if standPat >= beta then return beta
    var alpha = math.max(alpha0, standPat)

    val moves = new MoveList()
    MoveGen.generatePseudo(pos, moves, capturesOnly = true)
    orderCaptures(pos, moves)
    val mover = pos.sideToMove
    var i = 0
    while i < moves.size do
      val m = moves(i)
      val undo = pos.make(m)
      if pos.isAttacked(pos.kingSquare(mover), pos.sideToMove) then pos.unmake(m, undo)
      else
        val score = -quiescence(pos, -beta, -alpha)
        pos.unmake(m, undo)
        if score >= beta then return beta
        if score > alpha then alpha = score
      i += 1
    alpha

  private def isRepetition(pos: Position, ply: Int): Boolean =
    val current = historyLen + ply
    val stop = math.max(0, current - pos.halfmove)
    var i = current - 2
    while i >= stop do
      if pathHashes(i) == pos.hash then return true
      i -= 2
    false

  private def recordKiller(ply: Int, m: Int): Unit =
    if ply < maxPly && killers(ply)(0) != m then
      killers(ply)(1) = killers(ply)(0)
      killers(ply)(0) = m

  private def recordHistory(side: Int, m: Int, depth: Int): Unit =
    historyTable(side)(Move.from(m) * 128 + Move.to(m)) += depth * depth

  private def orderMoves(pos: Position, moves: MoveList, ttMove: Int, ply: Int): Unit =
    val n = moves.size
    val scores = new Array[Int](n)
    val side = pos.sideToMove
    val kply = if ply < maxPly then ply else maxPly - 1
    val k0 = killers(kply)(0)
    val k1 = killers(kply)(1)
    var i = 0
    while i < n do
      val m = moves(i)
      scores(i) =
        if m == ttMove && ttMove != 0 then 2000000
        else if Move.isCapture(m) then 1000000 + mvvLva(pos, m)
        else if Move.isPromotion(m) then 900000 + Eval.pieceValue(Move.promo(m))
        else if m == k0 && k0 != 0 then 800000
        else if m == k1 && k1 != 0 then 700000
        else historyTable(side)(Move.from(m) * 128 + Move.to(m))
      i += 1
    sortByScores(moves, scores)

  private def orderCaptures(pos: Position, moves: MoveList): Unit =
    val n = moves.size
    val scores = new Array[Int](n)
    var i = 0
    while i < n do
      val m = moves(i)
      scores(i) =
        if Move.isCapture(m) then mvvLva(pos, m)
        else if Move.isPromotion(m) then Eval.pieceValue(Move.promo(m))
        else 0
      i += 1
    sortByScores(moves, scores)

  private def sortByScores(moves: MoveList, scores: Array[Int]): Unit =
    val n = moves.size
    var i = 0
    while i < n do
      var bestIdx = i
      var j = i + 1
      while j < n do
        if scores(j) > scores(bestIdx) then bestIdx = j
        j += 1
      if bestIdx != i then
        val tmpMove = moves(i)
        moves(i) = moves(bestIdx)
        moves(bestIdx) = tmpMove
        val tmpScore = scores(i)
        scores(i) = scores(bestIdx)
        scores(bestIdx) = tmpScore
      i += 1

  private def mvvLva(pos: Position, m: Int): Int =
    val victim = if Move.isEnPassant(m) then Piece.Pawn else Piece.typeOf(pos.board(Move.to(m)))
    val attacker = Piece.typeOf(pos.board(Move.from(m)))
    Eval.pieceValue(victim) * 16 - Eval.pieceValue(attacker)

  private def extractPv(pos: Position, maxLen: Int): List[Int] =
    val out = List.newBuilder[Int]
    val trail = scala.collection.mutable.ArrayDeque[(Int, Undo)]()
    var steps = 0
    var continue = true
    while continue && steps < maxLen do
      val m = tt.probeMove(pos.hash)
      if m == 0 || !isLegal(pos, m) then continue = false
      else
        out += m
        trail.append((m, pos.make(m)))
        steps += 1
    while trail.nonEmpty do
      val (m, u) = trail.removeLast()
      pos.unmake(m, u)
    out.result()

  private def isLegal(pos: Position, m: Int): Boolean =
    val legal = new MoveList()
    MoveGen.generateLegal(pos, legal)
    var i = 0
    while i < legal.size do
      if legal(i) == m then return true
      i += 1
    false

object Searcher:
  val MateValue = 30000
  val Infinity = 1000000
  val DrawScore = 0
  val AspirationWindow = 25
