[![Build Status](https://travis-ci.org/evalapply/Swordfight.svg?branch=master)](https://travis-ci.org/evalapply/Swordfight)

# Swordfight


"*A game of chess is like a sword fight. You must think first before you move.*"
([Shaolin and Wu Tang](https://en.wikipedia.org/wiki/Shaolin_and_Wu_Tang))

Swordfight is a chess engine (no GUI, just a chess AI). It's CECP-compatible and
works with XBoard GUI.


## Usage

1. Download this repo.
2. Install [Leiningen](http://leiningen.org/) (requires Java JDK version 6 or
   later).
3. Install [XBoard](https://www.gnu.org/software/xboard/) (should be available
   in your favorite package manager).
4. Run `xboard` and choose `Engine -> Load New 1st Engine`.
   To add Swordfight as a new engine you need to point to the repo directory
   and set `lein run` as command. This will make Swordfight play as black.
5. Play.

Swordfight currently uses paralelized minimax algorithm with a very simple evaluation function. If you run `xboard -debug` there will be some debugging info in the `xboard.debug` file.

![XBoard window](https://raw.githubusercontent.com/evalapply/Swordfight/master/doc/mexican_defense.png)


You can also play without a GUI by running `lein run` and using a command line interface
like in the session below (user input is after the `>` prompt). A monospaced font in your
terminal is highly recommended for the debug output (chess board):

```
# {:xboard-mode false, :debug-mode true, :edition-current-color W}
# {:white-can-castle-ks true, :black-can-castle-qs true, :last-move [     ], :edited false, :turn W, :black-can-castle-ks true, :moves-cnt 0, :white-can-castle-qs true}
#
# 8  ♜ ♞ ♝ ♛ ♚ ♝ ♞ ♜
# 7  ♟ ♟ ♟ ♟ ♟ ♟ ♟ ♟
# 6                 
# 5                 
# 4                 
# 3                 
# 2  ♙ ♙ ♙ ♙ ♙ ♙ ♙ ♙
# 1  ♖ ♘ ♗ ♕ ♔ ♗ ♘ ♖
#
#    a b c d e f g h
#
> e2e4
# {:white-can-castle-ks true, :black-can-castle-qs true, :last-move [e2 e4], :edited false, :turn B, :black-can-castle-ks true, :moves-cnt 1, :white-can-castle-qs true}
#
# 8  ♜ ♞ ♝ ♛ ♚ ♝ ♞ ♜
# 7  ♟ ♟ ♟ ♟ ♟ ♟ ♟ ♟
# 6                 
# 5                 
# 4          ♙      
# 3                 
# 2  ♙ ♙ ♙ ♙   ♙ ♙ ♙
# 1  ♖ ♘ ♗ ♕ ♔ ♗ ♘ ♖
#
#    a b c d e f g h
#
move b8c6
# {:xboard-mode false, :debug-mode true, :edition-current-color W}
# {:white-can-castle-ks true, :black-can-castle-qs true, :last-move [b8 c6], :edited false, :turn W, :black-can-castle-ks true, :moves-cnt 2, :white-can-castle-qs true}
#
# 8  ♜   ♝ ♛ ♚ ♝ ♞ ♜
# 7  ♟ ♟ ♟ ♟ ♟ ♟ ♟ ♟
# 6      ♞          
# 5                 
# 4          ♙      
# 3                 
# 2  ♙ ♙ ♙ ♙   ♙ ♙ ♙
# 1  ♖ ♘ ♗ ♕ ♔ ♗ ♘ ♖
#
#    a b c d e f g h
#
>
```

I think the unicode chess pieces look pretty neat!

♜ ♖ ♞ ♘ ♝ ♗ ♛ ♕ ♚ ♔ ♟ ♙

Depending on the terminal the colors may be confusing. ♜ is black.

## Protocol

This engine will slowly become more and more
[CECP](https://en.wikipedia.org/wiki/Chess_Engine_Communication_Protocol)-compatible.
Basically what it means is it reads from standard input and writes to standard output
following a commonly used protocol, which is based on GNU Chess CLI and designed by
Tim Mann, the author of XBoard. It's quite possible that Swordfight will support
Universal Chess Interface or other protocols in the future.

Here's what [the author himself says about CECP](http://www.open-aurec.com/wbforum/WinBoard/engine-intf.html#4):
> Originally, xboard was just trying to talk to the existing command-line interface of GNU Chess 3.1+ and 4, which was designed for people to type commands to. So the communication protocol is very ad-hoc. It might have been good to redesign it early on, but because xboard and GNU Chess are separate programs, I didn't want to force people to upgrade them together to versions that matched. I particularly wanted to keep new versions of xboard working with old versions of GNU Chess, to make it easier to compare the play of old and new gnuchess versions. I didn't foresee the need for a clean protocol to be used with other chess engines in the future.

## Did you know?

The field of computer chess started with
[*Programming a Computer for Playing Chess*](http://archive.computerhistory.org/projects/chess/related_materials/text/2-0%20and%202-1.Programming_a_computer_for_playing_chess.shannon/2-0%20and%202-1.Programming_a_computer_for_playing_chess.shannon.062303002.pdf),
a 1950 paper by [Claude Shannon](http://en.wikipedia.org/wiki/Claude_Shannon). Why is it that old papers are better than new ones? I don't know.

Did you know that the number of possible positions in chess is bigger than the number of atoms in the observable universe? That's a lot of positions!

## License

Distributed under the 2-clause BSD license.
