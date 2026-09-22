package gambit

class BitboardsTest extends munit.FunSuite:

  test("file and rank masks cover eight squares each and intersect once") {
    for i <- 0 until 8 do
      assertEquals(Bitboards.count(Bitboards.fileMask(i)), 8)
      assertEquals(Bitboards.count(Bitboards.rankMask(i)), 8)
    for f <- 0 until 8; r <- 0 until 8 do
      assertEquals(Bitboards.count(Bitboards.fileMask(f) & Bitboards.rankMask(r)), 1)
  }

  test("adjacent files exclude the file itself") {
    assertEquals(Bitboards.adjacentFiles(0), Bitboards.fileMask(1))
    assertEquals(Bitboards.adjacentFiles(7), Bitboards.fileMask(6))
    assertEquals(Bitboards.adjacentFiles(3), Bitboards.fileMask(2) | Bitboards.fileMask(4))
    for f <- 0 until 8 do
      assertEquals(Bitboards.adjacentFiles(f) & Bitboards.fileMask(f), 0L)
  }

  test("white passed mask looks forward on three files") {
    val e4 = Square.rankOf(Square.make(4, 3)) * 8 + Square.fileOf(Square.make(4, 3))
    val mask = Bitboards.passedWhite(e4)
    assert((mask & Bitboards.bit(5 * 8 + 4)) != 0L, "e5 ahead is covered")
    assert((mask & Bitboards.bit(5 * 8 + 3)) != 0L, "d5 ahead is covered")
    assert((mask & Bitboards.bit(3 * 8 + 4)) == 0L, "same rank is not covered")
    assert((mask & Bitboards.bit(4 * 8 + 2)) == 0L, "distant file is not covered")
  }

  test("passed masks are color mirror images") {
    for s <- 0 until 64 do
      val mirror = (7 - Bitboards.rankOf(s)) * 8 + Bitboards.fileOf(s)
      assertEquals(Bitboards.count(Bitboards.passedWhite(s)), Bitboards.count(Bitboards.passedBlack(mirror)))
  }

  test("knight attack table matches a hand count in the corner and center") {
    assertEquals(Bitboards.count(Bitboards.knightAttacks(0)), 2)
    assertEquals(Bitboards.count(Bitboards.knightAttacks(27)), 8)
  }

  test("king ring is nine squares in the center and four in the corner") {
    assertEquals(Bitboards.count(Bitboards.kingRing(27)), 9)
    assertEquals(Bitboards.count(Bitboards.kingRing(0)), 4)
  }
