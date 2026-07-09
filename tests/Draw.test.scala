package gambit

class DrawTest extends munit.FunSuite:

  test("insufficient material for bare kings and lone minor") {
    assert(Draw.isInsufficientMaterial(Fen.parse("4k3/8/8/8/8/8/8/4K3 w - - 0 1")))
    assert(Draw.isInsufficientMaterial(Fen.parse("4k3/8/8/8/8/8/5B2/4K3 w - - 0 1")))
    assert(Draw.isInsufficientMaterial(Fen.parse("4k3/8/8/8/8/8/5N2/4K3 w - - 0 1")))
    assert(Draw.isInsufficientMaterial(Fen.parse("4k3/8/5b2/8/8/8/8/4K3 b - - 0 1")))
  }

  test("sufficient material is not flagged") {
    assert(!Draw.isInsufficientMaterial(Fen.parse("4k3/8/8/8/8/8/5R2/4K3 w - - 0 1")))
    assert(!Draw.isInsufficientMaterial(Fen.parse("4k3/8/8/8/8/8/4PP2/4K3 w - - 0 1")))
    assert(!Draw.isInsufficientMaterial(Fen.parse("4k3/5b2/8/8/8/8/5B2/4K3 w - - 0 1")))
    assert(!Draw.isInsufficientMaterial(Fen.parse(Fen.startPosition)))
  }

  test("threefold repetition counts the current position") {
    val h = Fen.parse(Fen.startPosition).hash
    val other = Fen.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1").hash
    assert(Draw.isThreefold(Vector(h, other, h, other, h), h))
    assert(!Draw.isThreefold(Vector(h, other, h, other), h))
  }

  test("fifty-move rule is a draw in search despite winning material") {
    val stalled = Fen.parse("7k/8/8/8/8/8/8/KQ6 w - - 100 1")
    assertEquals(new Searcher().search(stalled, 3, 0).score, 0)
    val fresh = Fen.parse("7k/8/8/8/8/8/8/KQ6 w - - 0 1")
    assert(new Searcher().search(fresh, 3, 0).score > 0, "with a live clock the queen must count")
  }

  test("repetition detection sees a matching ancestor at the right distance") {
    val pos = Fen.parse("R6k/8/8/8/8/8/8/6K1 w - - 6 30")
    val decoy = Fen.parse("R6k/8/8/8/8/8/8/5K2 w - - 6 30").hash
    assert(new Searcher().wouldRepeat(pos, Array(pos.hash, decoy)))
    assert(!new Searcher().wouldRepeat(pos, Array(decoy, decoy)))
    assert(!new Searcher().wouldRepeat(pos, Array(decoy, pos.hash)))
  }

  test("self-play from a dead endgame is adjudicated as a draw") {
    val insufficient = Game.selfPlay("4k3/8/8/8/8/8/5B2/4K3 w - - 0 1", 20, 4, 0)
    assertEquals(insufficient.result, "1/2-1/2 (insufficient material)")
    val fifty = Game.selfPlay("4k3/8/8/3p4/3P4/8/8/4K3 w - - 98 1", 200, 3, 0)
    assertEquals(fifty.result, "1/2-1/2 (fifty-move rule)")
  }
