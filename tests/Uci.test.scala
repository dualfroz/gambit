package gambit

class UciTest extends munit.FunSuite:

  test("uci handshake replies with id and uciok") {
    val out = new Uci().handle("uci")
    assertEquals(out, List("id name gambit", "id author gambit", "uciok"))
  }

  test("isready replies readyok") {
    assertEquals(new Uci().handle("isready"), List("readyok"))
  }

  test("position startpos with moves then go returns a legal bestmove") {
    val uci = new Uci()
    uci.handle("ucinewgame")
    uci.handle("position startpos moves e2e4 e7e5 g1f3")
    val out = uci.handle("go depth 4")
    val best = out.last
    assert(best.startsWith("bestmove "), s"got $best")
    val move = best.stripPrefix("bestmove ")
    assert(move.matches("[a-h][1-8][a-h][1-8][nbrq]?"), s"malformed move $move")
    assert(move != "0000", "expected a real move")
  }

  test("position fen with trailing moves is parsed and searched") {
    val uci = new Uci()
    uci.handle("position fen 4k3/8/8/8/8/8/3q4/4K1R1 w - - 0 1 moves g1g2")
    val res = uci.handle("go depth 4")
    assert(res.last.startsWith("bestmove "), res.mkString(" | "))
  }

  test("go reports mate for a forced mate in one") {
    val uci = new Uci()
    uci.handle("position fen 6k1/5ppp/8/8/8/8/8/R6K w - - 0 1")
    val out = uci.handle("go depth 3")
    assert(out.exists(_.contains("score mate 1")), out.mkString(" | "))
    assertEquals(out.last, "bestmove a1a8")
  }

  test("go reports mate in two and the mating first move") {
    val uci = new Uci()
    uci.handle("position fen 7k/R7/5K2/8/8/8/8/8 w - - 0 1")
    val out = uci.handle("go depth 5")
    assert(out.exists(_.contains("score mate 2")), out.mkString(" | "))
    assertEquals(out.last, "bestmove f6g6")
  }

  test("movetime search still returns a bestmove") {
    val uci = new Uci()
    uci.handle("position startpos")
    val out = uci.handle("go movetime 200")
    assert(out.last.startsWith("bestmove "))
    assert(out.nonEmpty)
  }

  test("run consumes a command stream up to quit") {
    val collected = scala.collection.mutable.ListBuffer[String]()
    val input = List("uci", "isready", "quit", "go depth 3").iterator
    new Uci().run(input, collected += _)
    assert(collected.contains("uciok"))
    assert(collected.contains("readyok"))
    assert(!collected.exists(_.startsWith("bestmove")), "commands after quit must be ignored")
  }
