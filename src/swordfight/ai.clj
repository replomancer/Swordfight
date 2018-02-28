(ns swordfight.ai
  (:require [swordfight.rules :refer [move legal-moves
                                      pseudolegal-moves
                                      squares-attacked-by
                                      pieces-attacked-by-opponent
                                      king-in-check?]]
            [swordfight.board :refer [all-coords change-side empty-square?
                                      white-king white-queen white-rook
                                      white-bishop white-knight white-pawn
                                      black-king black-queen black-rook
                                      black-bishop black-knight black-pawn
                                      empty-square white black
                                      piece-type ->piece move-piece
                                      notation->coords]]
            #_[taoensso.tufte :refer [defnp p profiled profile]]
            #_[swordfight.profiling]))

(def piece-value {white-king  20000 white-queen   900  white-rook  500
                  white-bishop  300 white-knight  300  white-pawn  100
                  black-king -20000 black-queen  -900  black-rook -500
                  black-bishop -300 black-knight -300  black-pawn -100
                  empty-square 0})

;;;  Tomasz Michniewski's  Simplified evaluation function
(def white-bonus-eval-tables
  {white-pawn
   [[  0   0   0   0   0   0   0   0 ]
    [ 50  50  50  50  50  50  50  50 ]
    [ 10  10  20  30  30  20  10  10 ]
    [  5   5  10  25  25  10   5   5 ]
    [  0   0   0  20  20   0   0   0 ]
    [  5  -5 -10   0   0 -10  -5   5 ]
    [  5  10  10 -20 -20  10  10   5 ]
    [  0   0   0   0   0   0   0   0 ]]

   white-knight
   [[-50 -40 -30 -30 -30 -30 -40 -50 ]
    [-40 -20   0   0   0   0 -20 -40 ]
    [-30   0  10  15  15  10   0 -30 ]
    [-30   5  15  20  20  15   5 -30 ]
    [-30   0  15  20  20  15   0 -30 ]
    [-30   5  10  15  15  10   5 -30 ]
    [-40 -20   0   5   5   0 -20 -40 ]
    [-50 -40 -30 -30 -30 -30 -40 -50 ]]

   white-bishop
   [[-20 -10 -10 -10 -10 -10 -10 -20 ]
    [-10   0   0   0   0   0   0 -10 ]
    [-10   0   5  10  10   5   0 -10 ]
    [-10   5   5  10  10   5   5 -10 ]
    [-10   0  10  10  10  10   0 -10 ]
    [-10  10  10  10  10  10  10 -10 ]
    [-10   5   0   0   0   0   5 -10 ]
    [-20 -10 -10 -10 -10 -10 -10 -20 ]]

   white-rook
   [[  0   0   0   0   0   0   0   0 ]
    [  5  10  10  10  10  10  10   5 ]
    [ -5   0   0   0   0   0   0  -5 ]
    [ -5   0   0   0   0   0   0  -5 ]
    [ -5   0   0   0   0   0   0  -5 ]
    [ -5   0   0   0   0   0   0  -5 ]
    [ -5   0   0   0   0   0   0  -5 ]
    [  0   0   0   5   5   0   0   0 ]]

   white-queen
   [[-20 -10 -10  -5  -5 -10 -10 -20 ]
    [-10   0   0   0   0   0   0 -10 ]
    [-10   0   5   5   5   5   0 -10 ]
    [ -5   0   5   5   5   5   0  -5 ]
    [  0   0   5   5   5   5   0  -5 ]
    [-10   5   5   5   5   5   0 -10 ]
    [-10   0   5   0   0   0   0 -10 ]
    [-20 -10 -10  -5  -5 -10 -10 -20 ]]

   white-king
   [[-30 -40 -40 -50 -50 -40 -40 -30 ]
    [-30 -40 -40 -50 -50 -40 -40 -30 ]
    [-30 -40 -40 -50 -50 -40 -40 -30 ]
    [-30 -40 -40 -50 -50 -40 -40 -30 ]
    [-20 -30 -30 -40 -40 -30 -30 -20 ]
    [-10 -20 -20 -20 -20 -20 -20 -10 ]
    [ 20  20   0   0   0   0  20  20 ]
    [ 20  30  10   0   0  10  30  20 ]]})

(def black-bonus-eval-tables
  (into {}
        (for [[c t] white-bonus-eval-tables]
          [(->piece black (piece-type c))
           (vec (reverse (map (fn [row] (mapv - row)) t)))])))

(def bonus-eval-tables
  (merge white-bonus-eval-tables black-bonus-eval-tables))

(defn piece-val-with-bonus [piece yx]
  (if (empty-square? piece)
    0
    (+ (piece-value piece) (get-in (bonus-eval-tables piece) yx))))

(defn eval-board [board]
  (reduce + (map (fn [[piece [y x]]]
                   (piece-val-with-bonus piece [y x]))
                 (for [[y x] all-coords]
                   [(get-in board [y x]) [y x]]))))

(defn board-value-delta [board board' move]
  (let [[move-from move-to] move
        [from-pos to-pos] [move-from (subs move-to 0 2)]
        [from-idx to-idx] (map notation->coords [from-pos to-pos])]
    (- (piece-val-with-bonus (get-in board' to-idx) to-idx)
       (piece-val-with-bonus (get-in board to-idx) to-idx)
       (piece-val-with-bonus (get-in board from-idx) from-idx))))

(def checkmated-val {white -100000  black 100000})
(defn stalemated-val [board] (/ (eval-board board) 4))

(def minimax-depth 4)

(def hurried-move (atom false))

(declare best-move)

(defn move-evaluation [game-state alpha beta depth piece-move]
  (let [state-after-move (move game-state piece-move)
        extra-board-changes (not= (:board state-after-move)
                                  (first (move-piece (:board game-state)
                                                     piece-move)))
        evaled-state-after-move (assoc state-after-move
                                       :board-value
                                       (if extra-board-changes
                                         ;; full board reevaluation only after
                                         ;; castling, en passant and promotions
                                         (eval-board (:board state-after-move))
                                         (+ (:board-value game-state)
                                            (board-value-delta (:board game-state)
                                                               (:board state-after-move)
                                                               piece-move))))
        move-value (if (or (= depth 1) @hurried-move)
                     (:board-value evaled-state-after-move)
                     (second
                      (best-move evaled-state-after-move alpha beta (dec depth))))]
    [piece-move move-value]))

(defn best-move [{:keys [board turn] :as game-state} alpha beta depth]
  (let [available-moves (if (= depth minimax-depth)
                          (legal-moves game-state)
                          ;; This is "cheating" which harms how well we predict
                          ;; but it's very useful for performance (speed)
                          (pseudolegal-moves game-state))
        mapping-fn ;; simple parallelization only for bigger cases
        (if (< (- minimax-depth depth) 2) pmap map)]
    (if (empty? available-moves)
      (if (king-in-check? game-state turn)
        [[:checkmated turn] (checkmated-val turn)]
        [[:stalemated turn] (stalemated-val board)])
      ;;  YBWC (Young Brothers Wait Concept)
      (let [first-move-evaluation
            (move-evaluation game-state alpha beta depth
                             (first available-moves))
            [alpha' beta'] (if (= turn white)
                             [(second first-move-evaluation) beta]
                             [alpha (second first-move-evaluation)])]
        (if (>= alpha' beta')
          first-move-evaluation
          (let [rest-evaluation
                (mapping-fn #(move-evaluation game-state alpha' beta' depth %)
                            (rest available-moves))
                evaluated-moves (cons first-move-evaluation rest-evaluation)]
            ;; move evaluation is second in pair
            (apply (if (= turn white) max-key min-key) second
                   evaluated-moves)))))))

(defn midgame-move [game-state]
  (best-move game-state Integer/MIN_VALUE Integer/MAX_VALUE minimax-depth))

(def early-game-turns 4)

(defn early-move [{turn :turn :as game-state}]
  (let [available-moves (legal-moves game-state)
        attacked-pieces (pieces-attacked-by-opponent game-state)]
    (if (seq attacked-pieces)
      (midgame-move game-state)
      (let [moves [["d7" "d5"] ["e7" "e5"] ["d7" "d6"] ["e7" "e6"]]
            good-moves (filter (fn [mv]
                                 (and (some #{mv} available-moves)
                                      (not-any? #{(second mv)}
                                                (squares-attacked-by
                                                 (move game-state mv)
                                                 (change-side turn)))))
                               moves)]
        (if (seq good-moves)
          [(first good-moves) nil]
          (midgame-move game-state))))))

(defn computer-move [game-state]
  (let [moves-cnt (:moves-cnt game-state)
        board-value (eval-board (:board game-state))
        evaled-board-game-state (assoc game-state :board-value board-value)
        [square-from square-to] (if (and (< moves-cnt early-game-turns)
                                         (false? (:edited game-state)))
                                  (early-move evaled-board-game-state)
                                  (midgame-move evaled-board-game-state))]
    [square-from square-to]))
