package gambit

class TimeControlTest extends munit.FunSuite:
  import TimeControl.Limits

  test("movetime is honored minus a safety overhead") {
    assertEquals(TimeControl.budgetMs(Limits(movetime = 200L), true), 175L)
  }

  test("infinite and depth-only searches carry no time budget") {
    assertEquals(TimeControl.budgetMs(Limits(infinite = true), true), 0L)
    assertEquals(TimeControl.budgetMs(Limits(depth = 12), true), 0L)
    assertEquals(TimeControl.budgetMs(Limits(), true), 0L)
  }

  test("a fixed number of moves to go splits the clock evenly") {
    assertEquals(TimeControl.budgetMs(Limits(wtime = 60000L, btime = 60000L, movestogo = 30), true), 1975L)
  }

  test("increment is folded into the per-move budget") {
    assertEquals(TimeControl.budgetMs(Limits(wtime = 1000L, winc = 1000L), true), 758L)
  }

  test("the side to move selects its own clock") {
    val limits = Limits(wtime = 60000L, btime = 5000L, movestogo = 10)
    assertEquals(TimeControl.budgetMs(limits, true), 5975L)
    assertEquals(TimeControl.budgetMs(limits, false), 475L)
  }

  test("very low time never returns a non-positive budget") {
    assert(TimeControl.budgetMs(Limits(wtime = 40L), true) >= 1L)
    assert(TimeControl.budgetMs(Limits(wtime = 0L, winc = 10L), true) >= 1L)
  }

  test("plan couples a depth cap with the time budget") {
    assertEquals(TimeControl.plan(Limits(depth = 8), true), TimeControl.Plan(8, 0L))
    assertEquals(TimeControl.plan(Limits(movetime = 1000L), true), TimeControl.Plan(64, 975L))
  }

  test("parse reads every clock token and ignores unknown ones") {
    val limits = TimeControl.parse("wtime 300000 btime 250000 winc 2000 binc 1500 movestogo 40 ponder".split(" ").toList)
    assertEquals(limits.wtime, 300000L)
    assertEquals(limits.btime, 250000L)
    assertEquals(limits.winc, 2000L)
    assertEquals(limits.binc, 1500L)
    assertEquals(limits.movestogo, 40)
  }
