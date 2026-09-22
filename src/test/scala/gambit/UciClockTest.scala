package gambit

class UciClockTest extends munit.FunSuite:

  test("go with a wtime/btime clock returns a legal bestmove") {
    val uci = new Uci()
    uci.handle("position startpos")
    val out = uci.handle("go wtime 3000 btime 3000 movestogo 20")
    val best = out.last
    assert(best.startsWith("bestmove "), out.mkString(" | "))
    val move = best.stripPrefix("bestmove ")
    assert(move.matches("[a-h][1-8][a-h][1-8][nbrq]?"), s"malformed move $move")
    assert(out.exists(_.startsWith("info depth ")), "clock search must still stream info")
  }

  test("go with an increment-only sudden-death clock still moves") {
    val uci = new Uci()
    uci.handle("position startpos moves e2e4 c7c5")
    val out = uci.handle("go wtime 1500 btime 1500 winc 100 binc 100")
    assert(out.last.startsWith("bestmove "))
    assert(out.last.stripPrefix("bestmove ") != "0000")
  }

  test("stop over the streaming loop aborts an infinite search and prints a bestmove") {
    val collected = scala.collection.mutable.ListBuffer[String]()
    val script = List[() => String](
      () => "position startpos",
      () => "go infinite",
      () => { Thread.sleep(200); "stop" },
      () => "quit"
    )
    val input = script.iterator.map(_())
    new Uci().run(input, collected += _)
    assert(collected.exists(_.startsWith("bestmove ")), collected.mkString(" | "))
    assert(collected.exists(_.startsWith("info depth ")), "an infinite search should report progress")
  }
