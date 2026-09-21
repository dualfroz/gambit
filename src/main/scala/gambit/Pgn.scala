package gambit

final case class PgnGame(
    tags: List[(String, String)],
    startFen: String,
    moves: List[Int],
    result: String
)

object Pgn:
  private val resultTokens = Set("1-0", "0-1", "1/2-1/2", "*")

  def write(
      startFen: String,
      moves: List[Int],
      result: String,
      tags: List[(String, String)] = Nil
  ): String =
    val header = tagSection(startFen, result, tags)
    val movetext = wrap(renderMoves(startFen, moves) :+ result)
    s"$header\n$movetext\n"

  private def tagSection(startFen: String, result: String, extra: List[(String, String)]): String =
    val provided = extra.map(_._1).toSet
    val roster = List(
      "Event" -> "gambit self-play",
      "Site" -> "local",
      "Date" -> "????.??.??",
      "Round" -> "-",
      "White" -> "gambit",
      "Black" -> "gambit",
      "Result" -> result
    ).filterNot(pair => provided.contains(pair._1))
    val setup =
      if startFen == Fen.startPosition then Nil
      else List("SetUp" -> "1", "FEN" -> startFen)
    val all = extra ++ roster ++ setup
    all.map((k, v) => s"""[$k "$v"]""").mkString("\n")

  private def renderMoves(startFen: String, moves: List[Int]): List[String] =
    val pos = Fen.parse(startFen)
    val out = scala.collection.mutable.ListBuffer[String]()
    var first = true
    var index = 0
    while index < moves.length do
      val m = moves(index)
      if pos.sideToMove == Color.White then out += s"${pos.fullmove}."
      else if first then out += s"${pos.fullmove}..."
      out += San.toSan(pos, m)
      pos.make(m)
      first = false
      index += 1
    out.toList

  private def wrap(tokens: List[String], width: Int = 80): String =
    val sb = new StringBuilder
    var lineLen = 0
    for token <- tokens do
      val add = if lineLen == 0 then token.length else token.length + 1
      if lineLen > 0 && lineLen + add > width then
        sb.append('\n')
        sb.append(token)
        lineLen = token.length
      else
        if lineLen > 0 then sb.append(' ')
        sb.append(token)
        lineLen += add
    sb.toString

  def read(text: String): PgnGame =
    val tags = parseTags(text)
    val startFen = tags.find(_._1 == "FEN").map(_._2).getOrElse(Fen.startPosition)
    val body = text.linesIterator.filterNot(_.trim.startsWith("[")).mkString(" ")
    val cleaned = stripDecorations(body)
    val tokens = cleaned.split("\\s+").filter(_.nonEmpty).toList
    val pos = Fen.parse(startFen)
    val moves = scala.collection.mutable.ListBuffer[Int]()
    var result = "*"
    for raw <- tokens do
      val token = raw.replaceAll("^\\d+\\.+", "")
      if token.isEmpty then ()
      else if resultTokens.contains(token) then result = token
      else
        val m = San.parse(pos, token)
        if m != 0 then
          moves += m
          pos.make(m)
    PgnGame(tags, startFen, moves.toList, result)

  private def parseTags(text: String): List[(String, String)] =
    val pattern = """\[(\w+)\s+"([^"]*)"\]""".r
    pattern.findAllMatchIn(text).map(m => (m.group(1), m.group(2))).toList

  private def stripDecorations(body: String): String =
    val noComments = removePaired(body, '{', '}')
    val noVariations = removePaired(noComments, '(', ')')
    noVariations.replaceAll("\\$\\d+", " ")

  private def removePaired(s: String, open: Char, close: Char): String =
    val sb = new StringBuilder
    var depth = 0
    var i = 0
    while i < s.length do
      val c = s.charAt(i)
      if c == open then depth += 1
      else if c == close then (if depth > 0 then depth -= 1)
      else if depth == 0 then sb.append(c)
      i += 1
    sb.toString
