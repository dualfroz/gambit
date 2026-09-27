# gambit

A chess engine written in Scala 3, built with [scala-cli](https://scala-cli.virtuslab.org/).

## Overview

`gambit` is a from-scratch alpha-beta chess engine. It uses a `0x88`
mailbox board with incremental Zobrist hashing, a tapered hand-tuned
evaluation and a principal-variation search with a transposition table,
quiescence search and the usual pruning, reduction and move-ordering
heuristics. It speaks the UCI protocol, so it can be loaded into any UCI
GUI, and it can read and write SAN, PGN and EPD.

## Features

- **Board**: `0x88` mailbox representation, make/unmake with an undo record,
  incremental Zobrist hashing of pieces, side to move, castling rights and
  the en passant file.
- **Move generation**: pseudo-legal generation for all pieces (castling
  through unattacked squares, en passant, promotions to all four pieces, a
  captures-only mode for quiescence) and legal filtering by make/unmake,
  validated with `perft` against the standard reference positions.
- **Evaluation**: tapered midgame/endgame score blended by game phase, with
  piece-square tables, pawn structure from 64-bit masks (passed pawns scaled
  by rank, doubled and isolated pawns), mobility, the bishop pair, rooks on
  open and semi-open files, and king safety (pawn shield and attacked squares
  around the king).
- **Static exchange evaluation**: full swap-off on a square, including x-ray
  attackers behind the first slider and a king that may not recapture into a
  defended square.
- **Search**: see [Search](#search) below.
- **Time management**: per-move budget from the UCI clock (`wtime`, `btime`,
  `winc`, `binc`, `movestogo`), `movetime`, `depth` and `infinite`, with a
  safety overhead and a cap on how much of the remaining clock one move may use.
- **UCI**: `uci`, `isready`, `ucinewgame`, `position startpos|fen ... moves ...`,
  `go`, `stop` and `quit`. The search runs on a worker thread, so `stop`
  interrupts it, and every finished iteration prints an `info` line with depth,
  score (`cp` or `mate`), nodes, nps, time and the principal variation.
- **Notation**: SAN output with disambiguation and check/mate suffixes, SAN
  parsing, PGN writing (Seven Tag Roster, `SetUp`/`FEN` for non-standard starts,
  80-column movetext) and PGN reading that skips comments, variations and NAGs.
- **EPD**: parser for `bm`, `am`, `id`, `hmvc` and `fmvn`, and a test-suite
  runner with 20 bundled tactical positions: WAC.001 to WAC.020 from Win at
  Chess, FEN and best move exactly as in the standard `wac.epd`.
- **Self-play**: a game loop that adjudicates checkmate, stalemate, the
  fifty-move rule, insufficient material and threefold repetition, and prints
  the game as PGN.

## Search

- Iterative deepening with principal-variation search at the root and in the tree.
- Aspiration windows (+-25 cp) from depth 4, with a full-window re-search on failure.
- Transposition table: 2^20 entries, depth-preferred replacement, exact/lower/upper
  bounds, mate scores stored relative to the node, best move reused for ordering
  and for extracting the principal variation.
- Check extensions.
- Null-move pruning (reduction 2 + depth/6) outside check and only with non-pawn
  material on the board.
- Reverse futility pruning and razoring near the leaves.
- Futility pruning and late-move pruning of quiet moves, with a tighter move limit
  when the static evaluation is not improving.
- Late-move reductions from a logarithmic table, reduced less for killer moves and
  more at non-PV nodes, with a re-search when a reduced move beats alpha.
- Move ordering: transposition-table move, MVV-LVA captures, promotions, two killer
  moves per ply, history heuristic.
- Quiescence search: stand pat, captures and promotions, delta pruning, pruning of
  captures that lose material by SEE, full check evasions with mate detection.
- Draw detection inside the search: repetitions along the search path and the game
  history (bounded by the halfmove clock) and the fifty-move rule.
- The clock is checked every 2048 nodes; a stop request from another thread ends the
  search and returns the best move of the last finished iteration.

## Performance

Measured on an Intel Core i7-14650HX, OpenJDK 17, single thread, run with
`nice -n 19`. Numbers move with the machine and the JVM.

Perft (leaf nodes at depth 1 are counted from the legal move list):

| Position | Depth and nodes | Speed |
|---|---|---|
| Start position | 6: 119,060,324 | 25.1 M nodes/s |
| Kiwipete | 5: 193,690,690 | 25.6 M nodes/s |
| Position 3 | 6: 11,030,083 | 13.9 M nodes/s |
| Position 4 | 5: 15,833,292 | 17.6 M nodes/s |
| Position 5 | 5: 89,941,194 | 20.8 M nodes/s |
| Position 6 | 5: 164,075,551 | 26.5 M nodes/s |

All counts match the published reference values.

Search speed over UCI, including quiescence nodes:

| Position | Search | Speed |
|---|---|---|
| Start position | `go depth 15`, 1.24 M nodes in 4.0 s | 314 k nodes/s |
| Kiwipete | `go depth 12`, 1.18 M nodes in 3.0 s | 389 k nodes/s |

With 1 second per position the EPD runner solves 19 of the 20 bundled positions.

## Usage

```sh
# UCI mode, for a chess GUI or a match runner
scala-cli run . -- uci

# self-play a game: maxPlies, depth, optional start FEN
scala-cli run . -- selfplay 200 6

# perft and a short search on a position
scala-cli run . -- startpos 5
scala-cli run . -- r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1 4

# EPD suite, N ms per position
scala-cli run . -- epd 500
```

Running with no arguments prints usage and a perft/search demo on the start
position.

To get a standalone launcher for a GUI:

```sh
scala-cli --power package . -o gambit --assembly
./gambit uci
```

## Tests

```sh
scala-cli test .
```

Tests use [munit](https://scalameta.org/munit/) and cover bitboard masks, FEN,
make/unmake and incremental hashing, move generation, perft (start position,
Kiwipete and positions 3 to 6, including a slower deep suite), evaluation terms,
SEE, the transposition table, search (mates, check extensions, null move, stop
handling, time limits), draw detection, SAN, PGN, time control, the UCI handler,
self-play and the legality of the bundled WAC best moves. The full run takes about 40 seconds, most of it in the deep perft
suite.

## Project layout

```
project.scala              scala-cli build: Scala version, main class, test dependency
src/main/scala/gambit/
  Types.scala              colors, pieces, 0x88 squares, castling masks, directions
  Move.scala               packed move encoding and MoveList
  Position.scala           board state, attack detection, make/unmake
  Zobrist.scala            hash keys
  Fen.scala                FEN parsing and output
  MoveGen.scala            pseudo-legal and legal move generation
  Perft.scala              perft and divide
  Bitboards.scala          64-bit masks used by the evaluation
  Eval.scala               static evaluation
  See.scala                static exchange evaluation
  Search.scala             iterative deepening, PVS, quiescence, move ordering
  SearchParams.scala       pruning and reduction parameters
  TT.scala                 transposition table
  Draw.scala               draw rules
  TimeControl.scala        UCI limits and time budget
  Uci.scala                UCI protocol handler
  San.scala, Pgn.scala     notation
  Epd.scala                EPD parsing and suite runner
  WinAtChess.scala         bundled EPD positions
  Game.scala               self-play loop
  Main.scala               command-line entry point
src/test/scala/gambit/     munit suites
```

## Limitations

- Single-threaded search, no pondering, no UCI options (the hash size is fixed
  at 2^20 entries, about 19 MB).
- No opening book, no endgame tablebases, no Chess960.
- The evaluation is hand-tuned, not trained.
- Speed is modest for a modern engine: legality is checked by make/unmake of
  every pseudo-legal move and king safety scans attack rays in the evaluation.
- `go` ignores `searchmoves`, `ponder`, `nodes` and `mate`.

## Requirements

- scala-cli; Scala `3.8.4` is pinned in `project.scala` and fetched automatically.
- A JDK 17 or newer.

## License

MIT, see [LICENSE](LICENSE).
