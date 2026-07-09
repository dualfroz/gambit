# gambit

A chess engine written in Scala 3, built with [scala-cli](https://scala-cli.virtuslab.org/).

## Overview

`gambit` is a from-scratch alpha-beta chess engine. It uses a `0x88`
mailbox board representation, Zobrist hashing for transposition detection,
and a principal-variation search with a full set of modern pruning and
move-ordering heuristics. It speaks the UCI protocol and can also read and
write SAN, PGN and EPD.

## Features

- **Board** — `0x88` mailbox representation with incremental Zobrist hashing.
- **Move generation** — pseudo-legal and legal generation for all piece
  types, including castling, en passant and promotions, validated by a
  `perft` node counter.
- **Evaluation** — tapered midgame/endgame evaluation with piece-square
  tables, pawn structure (passed, doubled, isolated), mobility, the bishop
  pair, rook file bonuses and king safety, plus a static exchange
  evaluation (SEE).
- **Search** — iterative-deepening PVS with quiescence search, a
  transposition table, null-move pruning, reverse futility and razoring,
  late-move reductions, late-move pruning, killer moves and a history
  heuristic, with aspiration windows at the root.
- **Time management** — UCI `go` limit parsing (clock, increment,
  `movestogo`, `movetime`, `depth`, `infinite`) with a per-move budget.
- **Notation** — SAN formatting/parsing, PGN reading/writing, and EPD
  parsing with a test-suite runner (bundled Win at Chess positions).
- **Self-play** — a game loop with draw adjudication (fifty-move,
  insufficient material, threefold repetition).

## Usage

```sh
# UCI mode
scala-cli run . -- uci

# self-play a game (maxPlies, depth)
scala-cli run . -- selfplay 200 6

# perft / search on a position
scala-cli run . -- <fen|startpos> <depth>

# run the EPD (Win at Chess) suite, N ms per position
scala-cli run . -- epd 500
```

Running with no arguments prints usage and a short perft/search demo.

## Tests

```sh
scala-cli test .
```

Tests use [munit](https://scalameta.org/munit/) and cover move generation
and perft, make/unmake and hashing, evaluation and SEE, search heuristics,
the transposition table, draw detection, SAN/PGN notation, time control and
the UCI handler.

## Requirements

- scala-cli (Scala `3.8.4`, as pinned in `project.scala`)
