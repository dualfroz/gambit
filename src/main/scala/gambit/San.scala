package gambit

object San:
  private def pieceLetter(pieceType: Int): String = pieceType match
    case Piece.Knight => "N"
    case Piece.Bishop => "B"
    case Piece.Rook   => "R"
    case Piece.Queen  => "Q"
    case Piece.King   => "K"
    case _            => ""

  private def letterToType(ch: Char): Int = ch match
    case 'N' => Piece.Knight
    case 'B' => Piece.Bishop
    case 'R' => Piece.Rook
    case 'Q' => Piece.Queen
    case 'K' => Piece.King
    case _   => Piece.Empty

  def toSan(pos: Position, move: Int): String =
    val from = Move.from(move)
    val to = Move.to(move)
    val pieceType = Piece.typeOf(pos.board(from))
    val body =
      if Move.isCastle(move) then
        if Square.fileOf(to) == 6 then "O-O" else "O-O-O"
      else if pieceType == Piece.Pawn then
        val core =
          if Move.isCapture(move) then s"${('a' + Square.fileOf(from)).toChar}x${Square.name(to)}"
          else Square.name(to)
        if Move.isPromotion(move) then s"$core=${pieceLetter(Move.promo(move))}" else core
      else
        val capture = if Move.isCapture(move) then "x" else ""
        s"${pieceLetter(pieceType)}${disambiguation(pos, move, pieceType, from, to)}$capture${Square.name(to)}"
    body + suffix(pos, move)

  private def disambiguation(pos: Position, move: Int, pieceType: Int, from: Int, to: Int): String =
    val legal = new MoveList()
    MoveGen.generateLegal(pos, legal)
    var sameFile = false
    var sameRank = false
    var rival = false
    var i = 0
    while i < legal.size do
      val other = legal(i)
      if other != move && Move.to(other) == to && Piece.typeOf(pos.board(Move.from(other))) == pieceType then
        rival = true
        if Square.fileOf(Move.from(other)) == Square.fileOf(from) then sameFile = true
        if Square.rankOf(Move.from(other)) == Square.rankOf(from) then sameRank = true
      i += 1
    if !rival then ""
    else if !sameFile then s"${('a' + Square.fileOf(from)).toChar}"
    else if !sameRank then s"${('1' + Square.rankOf(from)).toChar}"
    else Square.name(from)

  private def suffix(pos: Position, move: Int): String =
    val undo = pos.make(move)
    val opponent = pos.sideToMove
    val check = pos.inCheck(opponent)
    val replies = new MoveList()
    MoveGen.generateLegal(pos, replies)
    val out = if check && replies.size == 0 then "#" else if check then "+" else ""
    pos.unmake(move, undo)
    out

  def parse(pos: Position, text: String): Int =
    var s = text.trim
    while s.nonEmpty && "+#!?".indexOf(s.last) >= 0 do s = s.dropRight(1)
    if s == "O-O" || s == "0-0" then castleMove(pos, 6)
    else if s == "O-O-O" || s == "0-0-0" then castleMove(pos, 2)
    else
      var promo = 0
      val eq = s.indexOf('=')
      if eq >= 0 then
        promo = letterToType(s(eq + 1))
        s = s.take(eq)
      val isPawn = letterToType(s(0)) == Piece.Empty
      val pieceType = if isPawn then Piece.Pawn else letterToType(s(0))
      var body = if isPawn then s else s.drop(1)
      body = body.filter(_ != 'x')
      val dest = Square.fromName(body.takeRight(2))
      val hint = body.dropRight(2)
      var hintFile = -1
      var hintRank = -1
      for ch <- hint do
        if ch >= 'a' && ch <= 'h' then hintFile = ch - 'a'
        if ch >= '1' && ch <= '8' then hintRank = ch - '1'
      matchMove(pos, pieceType, dest, promo, hintFile, hintRank)

  private def castleMove(pos: Position, toFile: Int): Int =
    val legal = new MoveList()
    MoveGen.generateLegal(pos, legal)
    var i = 0
    while i < legal.size do
      val m = legal(i)
      if Move.isCastle(m) && Square.fileOf(Move.to(m)) == toFile then return m
      i += 1
    0

  private def matchMove(pos: Position, pieceType: Int, dest: Int, promo: Int, hintFile: Int, hintRank: Int): Int =
    val legal = new MoveList()
    MoveGen.generateLegal(pos, legal)
    var i = 0
    while i < legal.size do
      val m = legal(i)
      val from = Move.from(m)
      if Piece.typeOf(pos.board(from)) == pieceType && Move.to(m) == dest
        && (promo == 0 || Move.promo(m) == promo)
        && (hintFile < 0 || Square.fileOf(from) == hintFile)
        && (hintRank < 0 || Square.rankOf(from) == hintRank)
      then return m
      i += 1
    0
