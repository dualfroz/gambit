package gambit

class EvalTest extends munit.FunSuite:
  private def e(fen: String): Int = Eval.evaluate(Fen.parse(fen))

  test("start position and a mirrored middlegame evaluate to zero") {
    assertEquals(e(Fen.startPosition), 0)
    assertEquals(e("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR w KQkq - 0 1"), 0)
  }

  test("doubled and isolated pawns are penalised against a clean structure") {
    val clean = e("4k3/8/8/8/8/8/1PP5/4K3 w - - 0 1")
    val doubled = e("4k3/8/8/8/8/1P6/1P6/4K3 w - - 0 1")
    assert(clean > doubled, s"clean $clean should beat doubled $doubled")
  }

  test("a centralised knight is worth more than a cornered one") {
    val center = e("4k3/8/8/8/4N3/8/8/4K3 w - - 0 1")
    val corner = e("4k3/8/8/8/8/8/8/N3K3 w - - 0 1")
    assert(center > corner, s"center $center should beat corner $corner")
  }

  test("the bishop pair scores above two knights") {
    val bishops = e("4k3/8/8/8/8/8/8/2B1KB2 w - - 0 1")
    val knights = e("4k3/8/8/8/8/8/8/1N2KN2 w - - 0 1")
    assert(bishops > knights, s"bishop pair $bishops should beat knight pair $knights")
  }

  test("an exposed king is worse than a king behind its pawn shield") {
    val sheltered = e("6k1/5ppp/8/8/8/8/5PPP/6K1 w - - 0 1")
    val exposed = e("5k2/5ppp/8/8/8/8/5PPP/K7 w - - 0 1")
    assert(exposed < sheltered, s"exposed $exposed should be worse than sheltered $sheltered")
  }

  test("an enemy queen next to the king adds king danger") {
    val far = e("6k1/5ppp/8/8/q7/8/5PPP/6K1 w - - 0 1")
    val near = e("6k1/5ppp/8/8/8/5q2/5PPP/6K1 w - - 0 1")
    assert(near < far, s"queen next to the king ($near) should score worse than far ($far)")
  }

  test("a passed pawn is worth more than a blockaded one") {
    val passed = e("4k3/2p4p/8/4P3/8/8/P7/4K3 w - - 0 1")
    val blocked = e("4k3/7p/4p3/4P3/8/8/P7/4K3 w - - 0 1")
    assert(passed > blocked, s"passed $passed should beat blocked $blocked")
  }

  test("a rook on an open file scores above one behind its own pawn") {
    val open = e("3rk3/p7/8/8/8/8/P7/3RK3 w - - 0 1")
    val closed = e("3rk3/p7/8/8/8/8/3P4/3RK3 w - - 0 1")
    assert(closed < open, s"closed file $closed should be worse than open $open")
  }
