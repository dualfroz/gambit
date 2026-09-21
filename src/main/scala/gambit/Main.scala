package gambit

@main def gambit(args: String*): Unit =
  args.toList match
    case Nil                => runDemo()
    case "uci" :: _         => new Uci().run(scala.io.Source.stdin.getLines(), println)
    case "selfplay" :: opts => runSelfPlay(opts)
    case "epd" :: opts      => runEpd(opts)
    case other              => runPerft(other)

private def runEpd(opts: List[String]): Unit =
  val timeMs = opts.headOption.map(_.toLong).getOrElse(500L)
  println(s"EPD test-suite runner: ${WinAtChess.positions.length} positions, ${timeMs}ms each")
  val t0 = System.nanoTime()
  val results = Epd.runSuite(WinAtChess.positions, timeMs)
  val ms = (System.nanoTime() - t0) / 1000000
  print(Epd.report(results))
  println(s"total wall time ${ms} ms")

private def runDemo(): Unit =
  println("gambit chess engine")
  println("usage:")
  println("  scala-cli run . -- uci")
  println("  scala-cli run . -- selfplay [maxPlies] [depth] [fen]")
  println("  scala-cli run . -- epd [msPerPosition]")
  println("  scala-cli run . -- <fen|startpos> <depth>")
  println()
  runPerft(List("startpos", "4"))

private def runPerft(args: List[String]): Unit =
  val depth = args.last.toInt
  val fenArg = args.dropRight(1).mkString(" ")
  val fen = if fenArg.isEmpty || fenArg == "startpos" then Fen.startPosition else fenArg
  val pos = Fen.parse(fen)
  println(s"fen:  ${Fen.toFen(pos)}")
  println(s"side: ${if pos.sideToMove == Color.White then "white" else "black"}")

  val t0 = System.nanoTime()
  val nodes = Perft.perft(pos, depth)
  val ms = (System.nanoTime() - t0) / 1000000
  val nps = if ms > 0 then nodes * 1000 / ms else 0
  println(s"perft($depth) = $nodes  (${ms} ms, ${nps} nps)")

  val searchDepth = math.min(math.max(depth, 4), 6)
  val searcher = new Searcher
  val searchPos = Fen.parse(fen)
  val r = searcher.search(searchPos, searchDepth, 0)
  println(s"bestmove ${Move.uci(r.bestMove)}  score ${r.score}  (depth ${r.depth}, ${searcher.nodes} nodes)")

private def runSelfPlay(opts: List[String]): Unit =
  val maxPlies = opts.headOption.map(_.toInt).getOrElse(200)
  val depth = opts.drop(1).headOption.map(_.toInt).getOrElse(5)
  val fenArg = opts.drop(2).mkString(" ")
  val fen = if fenArg.isEmpty then Fen.startPosition else fenArg
  println(s"self-play: maxPlies=$maxPlies depth=$depth")
  println(s"start: $fen")
  val outcome = Game.selfPlay(fen, maxPlies, depth, 0)
  println(s"moves (${outcome.plies}): ${outcome.moves.mkString(" ")}")
  println(s"result: ${outcome.result}")
  println()
  println(Game.pgn(outcome))
