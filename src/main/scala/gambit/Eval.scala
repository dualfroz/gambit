package gambit

object Eval:
  val pieceValue: Array[Int] = Array(0, 100, 320, 330, 500, 900, 20000)

  private val mgValue = Array(0, 100, 320, 330, 500, 900, 0)
  private val egValue = Array(0, 120, 320, 330, 540, 940, 0)

  private val phaseWeight = Array(0, 0, 1, 1, 2, 4, 0)
  private val maxPhase = 24

  private val pawnMg = Array(
    0, 0, 0, 0, 0, 0, 0, 0,
    50, 50, 50, 50, 50, 50, 50, 50,
    10, 10, 20, 30, 30, 20, 10, 10,
    5, 5, 10, 25, 25, 10, 5, 5,
    0, 0, 0, 20, 20, 0, 0, 0,
    5, -5, -10, 0, 0, -10, -5, 5,
    5, 10, 10, -20, -20, 10, 10, 5,
    0, 0, 0, 0, 0, 0, 0, 0
  )

  private val pawnEg = Array(
    0, 0, 0, 0, 0, 0, 0, 0,
    80, 80, 80, 80, 80, 80, 80, 80,
    50, 50, 50, 50, 50, 50, 50, 50,
    30, 30, 30, 30, 30, 30, 30, 30,
    20, 20, 20, 20, 20, 20, 20, 20,
    10, 10, 10, 10, 10, 10, 10, 10,
    10, 10, 10, 10, 10, 10, 10, 10,
    0, 0, 0, 0, 0, 0, 0, 0
  )

  private val knightTable = Array(
    -50, -40, -30, -30, -30, -30, -40, -50,
    -40, -20, 0, 0, 0, 0, -20, -40,
    -30, 0, 10, 15, 15, 10, 0, -30,
    -30, 5, 15, 20, 20, 15, 5, -30,
    -30, 0, 15, 20, 20, 15, 0, -30,
    -30, 5, 10, 15, 15, 10, 5, -30,
    -40, -20, 0, 5, 5, 0, -20, -40,
    -50, -40, -30, -30, -30, -30, -40, -50
  )

  private val bishopTable = Array(
    -20, -10, -10, -10, -10, -10, -10, -20,
    -10, 0, 0, 0, 0, 0, 0, -10,
    -10, 0, 5, 10, 10, 5, 0, -10,
    -10, 5, 5, 10, 10, 5, 5, -10,
    -10, 0, 10, 10, 10, 10, 0, -10,
    -10, 10, 10, 10, 10, 10, 10, -10,
    -10, 5, 0, 0, 0, 0, 5, -10,
    -20, -10, -10, -10, -10, -10, -10, -20
  )

  private val rookTable = Array(
    0, 0, 0, 0, 0, 0, 0, 0,
    5, 10, 10, 10, 10, 10, 10, 5,
    -5, 0, 0, 0, 0, 0, 0, -5,
    -5, 0, 0, 0, 0, 0, 0, -5,
    -5, 0, 0, 0, 0, 0, 0, -5,
    -5, 0, 0, 0, 0, 0, 0, -5,
    -5, 0, 0, 0, 0, 0, 0, -5,
    0, 0, 0, 5, 5, 0, 0, 0
  )

  private val queenTable = Array(
    -20, -10, -10, -5, -5, -10, -10, -20,
    -10, 0, 0, 0, 0, 0, 0, -10,
    -10, 0, 5, 5, 5, 5, 0, -10,
    -5, 0, 5, 5, 5, 5, 0, -5,
    0, 0, 5, 5, 5, 5, 0, -5,
    -10, 5, 5, 5, 5, 5, 0, -10,
    -10, 0, 5, 0, 0, 0, 0, -10,
    -20, -10, -10, -5, -5, -10, -10, -20
  )

  private val kingMg = Array(
    -30, -40, -40, -50, -50, -40, -40, -30,
    -30, -40, -40, -50, -50, -40, -40, -30,
    -30, -40, -40, -50, -50, -40, -40, -30,
    -30, -40, -40, -50, -50, -40, -40, -30,
    -20, -30, -30, -40, -40, -30, -30, -20,
    -10, -20, -20, -20, -20, -20, -20, -10,
    20, 20, 0, 0, 0, 0, 20, 20,
    20, 30, 10, 0, 0, 10, 30, 20
  )

  private val kingEg = Array(
    -50, -40, -30, -20, -20, -30, -40, -50,
    -30, -20, -10, 0, 0, -10, -20, -30,
    -30, -10, 20, 30, 30, 20, -10, -30,
    -30, -10, 30, 40, 40, 30, -10, -30,
    -30, -10, 30, 40, 40, 30, -10, -30,
    -30, -10, 20, 30, 30, 20, -10, -30,
    -30, -30, 0, 0, 0, 0, -30, -30,
    -50, -30, -30, -30, -30, -30, -30, -50
  )

  private val mgTables = Array(null, pawnMg, knightTable, bishopTable, rookTable, queenTable, kingMg)
  private val egTables = Array(null, pawnEg, knightTable, bishopTable, rookTable, queenTable, kingEg)

  private val passedMg = Array(0, 8, 16, 28, 48, 78, 120, 0)
  private val passedEg = Array(0, 18, 30, 52, 90, 140, 210, 0)
  private val doubledPenalty = 14
  private val isolatedPenalty = 12
  private val bishopPairBonus = 32
  private val rookOpenBonus = 22
  private val rookSemiOpenBonus = 11
  private val mobilityWeight = Array(0, 0, 4, 5, 3, 2, 0)
  private val shieldBonus = 9
  private val ringAttackPenalty = 8

  inline private def pstIndex(color: Int, sq: Int): Int =
    if color == Color.White then (7 - Square.rankOf(sq)) * 8 + Square.fileOf(sq)
    else Square.rankOf(sq) * 8 + Square.fileOf(sq)

  def evaluate(pos: Position): Int =
    val board = pos.board
    val pawns = Array(0L, 0L)
    val bishops = Array(0, 0)
    var mg = 0
    var eg = 0
    var phase = 0
    var flat = 0

    var sq = 0
    while sq < 128 do
      if Square.onBoard(sq) then
        val p = board(sq)
        if p != Piece.Empty then
          val pieceType = p & 7
          val color = p >> 3
          val idx = pstIndex(color, sq)
          val mgTerm = mgValue(pieceType) + mgTables(pieceType)(idx)
          val egTerm = egValue(pieceType) + egTables(pieceType)(idx)
          phase += phaseWeight(pieceType)
          if color == Color.White then
            mg += mgTerm
            eg += egTerm
          else
            mg -= mgTerm
            eg -= egTerm
          pieceType match
            case Piece.Pawn   => pawns(color) |= Bitboards.bit(Bitboards.sq64(sq))
            case Piece.Bishop => bishops(color) += 1
            case _            => ()
      sq += 1

    flat += mobility(pos, Color.White) - mobility(pos, Color.Black)
    flat += kingSafety(pos, Color.White) - kingSafety(pos, Color.Black)
    if bishops(Color.White) >= 2 then flat += bishopPairBonus
    if bishops(Color.Black) >= 2 then flat -= bishopPairBonus
    flat += rookFiles(pos, Color.White, pawns) - rookFiles(pos, Color.Black, pawns)

    val (pawnMgTerm, pawnEgTerm) = pawnStructure(pawns)
    mg += pawnMgTerm + flat
    eg += pawnEgTerm + flat

    val boundedPhase = if phase > maxPhase then maxPhase else phase
    val blended = (mg * boundedPhase + eg * (maxPhase - boundedPhase)) / maxPhase
    if pos.sideToMove == Color.White then blended else -blended

  private def pawnStructure(pawns: Array[Long]): (Int, Int) =
    var mg = 0
    var eg = 0
    var color = 0
    while color < 2 do
      val sign = if color == Color.White then 1 else -1
      val own = pawns(color)
      val enemy = pawns(color ^ 1)
      var f = 0
      while f < 8 do
        val onFile = Bitboards.count(own & Bitboards.fileMask(f))
        if onFile > 1 then
          mg -= sign * doubledPenalty * (onFile - 1)
          eg -= sign * doubledPenalty * (onFile - 1)
        if onFile > 0 && (own & Bitboards.adjacentFiles(f)) == 0L then
          mg -= sign * isolatedPenalty * onFile
          eg -= sign * isolatedPenalty * onFile
        f += 1
      var bb = own
      while bb != 0L do
        val s = java.lang.Long.numberOfTrailingZeros(bb)
        bb &= bb - 1
        val ahead = if color == Color.White then Bitboards.passedWhite(s) else Bitboards.passedBlack(s)
        if (ahead & enemy) == 0L then
          val relRank = if color == Color.White then Bitboards.rankOf(s) else 7 - Bitboards.rankOf(s)
          mg += sign * passedMg(relRank)
          eg += sign * passedEg(relRank)
      color += 1
    (mg, eg)

  private def mobility(pos: Position, color: Int): Int =
    val board = pos.board
    var total = 0
    var sq = 0
    while sq < 128 do
      if Square.onBoard(sq) then
        val p = board(sq)
        if p != Piece.Empty && (p >> 3) == color then
          val pt = p & 7
          pt match
            case Piece.Knight => total += stepCount(board, sq, color, Directions.knight) * mobilityWeight(pt)
            case Piece.Bishop => total += slideCount(board, sq, color, Directions.bishop) * mobilityWeight(pt)
            case Piece.Rook   => total += slideCount(board, sq, color, Directions.rook) * mobilityWeight(pt)
            case Piece.Queen =>
              val q = slideCount(board, sq, color, Directions.bishop) + slideCount(board, sq, color, Directions.rook)
              total += q * mobilityWeight(pt)
            case _ => ()
      sq += 1
    total

  private def stepCount(board: Array[Int], sq: Int, color: Int, offsets: Array[Int]): Int =
    var c = 0
    var i = 0
    while i < offsets.length do
      val t = sq + offsets(i)
      if Square.onBoard(t) && (board(t) == Piece.Empty || (board(t) >> 3) != color) then c += 1
      i += 1
    c

  private def slideCount(board: Array[Int], sq: Int, color: Int, offsets: Array[Int]): Int =
    var c = 0
    var i = 0
    while i < offsets.length do
      var t = sq + offsets(i)
      var stop = false
      while Square.onBoard(t) && !stop do
        val tp = board(t)
        if tp == Piece.Empty then
          c += 1
          t += offsets(i)
        else
          if (tp >> 3) != color then c += 1
          stop = true
      i += 1
    c

  private def kingSafety(pos: Position, color: Int): Int =
    val kingSq = pos.kingSquare(color)
    val enemy = color ^ 1
    val board = pos.board
    var score = 0
    val forward = if color == Color.White then 16 else -16
    val ownPawn = Piece.make(color, Piece.Pawn)
    var d = -1
    while d <= 1 do
      val shieldSq = kingSq + forward + d
      if Square.onBoard(shieldSq) && board(shieldSq) == ownPawn then score += shieldBonus
      val shieldSq2 = kingSq + forward * 2 + d
      if Square.onBoard(shieldSq2) && board(shieldSq2) == ownPawn then score += shieldBonus / 2
      d += 1

    var attackers = 0
    var i = 0
    while i < Directions.king.length do
      val ring = kingSq + Directions.king(i)
      if Square.onBoard(ring) && pos.isAttacked(ring, enemy) then attackers += 1
      i += 1
    score - attackers * ringAttackPenalty

  private def rookFiles(pos: Position, color: Int, pawns: Array[Long]): Int =
    val board = pos.board
    val rook = Piece.make(color, Piece.Rook)
    var score = 0
    var sq = 0
    while sq < 128 do
      if Square.onBoard(sq) && board(sq) == rook then
        val f = Square.fileOf(sq)
        val own = pawns(color) & Bitboards.fileMask(f)
        val enemy = pawns(color ^ 1) & Bitboards.fileMask(f)
        if own == 0L && enemy == 0L then score += rookOpenBonus
        else if own == 0L then score += rookSemiOpenBonus
      sq += 1
    score
