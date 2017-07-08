(ns flappy-bird.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.ui :refer :all]
            [play-clj.repl :refer :all]
            [play-clj.math :refer :all]))

(declare main-screen start-screen black-screen flappy-bird-game text-screen)
(def speed 50)
(def gravity-force 10)

(def game-state (atom {:elapsed-time 0}))


(defn to-radians [a]
  (/ (* a Math/PI) 180))

(defn square [n]
  (* n 2))

(defn gravity [entity]
  (assoc entity :y (+ (:y entity)
                      (* gravity-force
                         (Math/sin
                           (to-radians (:angle entity)))))))


(defn rotate [entity]
  (if (> (:angle entity) -90)
    (assoc entity :angle (- (:angle entity) 2))
    entity))


(defn check-still-flying [entity]
  (if (< (:angle entity) 0)
    (assoc entity :flying false)
    entity))


(defn fly [entity]
  (if (:flying entity)
    (assoc entity :y (+ (:y entity) (* 5 (Math/sin (to-radians (:angle entity))))))

    entity))

(defn animate [entity]
    (merge entity
           (cond
             (:flying entity) (:flying-im entity)
             (and (< (:angle entity) 10) (> (:angle entity) -10)) (:normal-im entity)
             :else (:falling-im entity))))

(defn start-game! []
  (reset! game-state {:elapsed-time 0})
  (app! :post-runnable #(set-screen! flappy-bird-game main-screen text-screen)))

(defn reset-game! []
  (app! :post-runnable #(set-screen! flappy-bird-game start-screen text-screen)))

(defn reset-when-dead [entity]
  (when (not (:alive entity))
      (reset-game!))
  entity)


(defn insert-into [coll item]
  (let [[low high] (split-at (- (count coll) 2) coll)]
    (vec (concat low [item] high))))


(defn update-hit-box [entity]
  (cond
    (:bird? entity)
    (assoc entity :hit-box (circle (+ (:x entity) 17) (+ (+ (:y entity) 10) (* 20 (Math/sin (to-radians (:angle entity)))))  13))
    (:pipe? entity)
    (assoc entity :hit-box (rectangle (:x entity) (:y entity) (:width entity) (:height entity)))
    :else entity))

(defn add-hit-boxes [entities]
  "Debug function"
  (->> entities
       (filter #(not (:hit-box? %)))
       (reduce #(if-let [hit-box (:hit-box %2)]
                    (do
;;                       (println hit-box)
;;                       (println (.x hit-box))
                      (conj %1 %2
                                  (if (:bird? %2)
                                    (assoc (shape :filled
                                                   :set-color (color 1 0 0 1)
                                                   :circle (.x hit-box) (.y hit-box) (.radius hit-box))
                                       :hit-box? true))))
;;                                     (assoc (shape :line
;;                                                    :set-color (color 1 0 0 1)
;;                                                    :rect (.x hit-box) (.y hit-box) (.width hit-box) (.height hit-box))
;;                                        :hit-box? true))))
                    (conj %1 %2))
               [])))

(defn abs [n]
  (max n (- n) 1))

(defn rand-pipe [etime]
  (let [r (- 100 (rand-int etime))]

    (cond
      (< r -90) -90
      (< 105 r) 105
      :else r)))

(defn recycle-pipe [shared-rand entity]
  (let [ry (- shared-rand 100)
;;         _ (clojure.pprint/pprint ry)
        pipe-down-y (+ ry 400)
        pipe-up-y ry]
;;     (clojure.pprint/pprint (str pipe-up-y "," pipe-down-y))
    (if (:up? entity)
      (assoc entity :x 380 :y pipe-down-y)
      (assoc entity :x 380 :y pipe-up-y))))

;; (def hit (update-hit-boxes {:bird? true :x 100 :y 10}))
;; (on-gl (println (add-hit-boxes [hit])))
;; (let [hitb (:hit-box)]
;;   (shape :rect (:x hitb)))
;; (update-hit-boxes {:pipe? true :x 100 :y 10})
(defn detect-colision [bird pipes]
  (if (or (< (:y bird) 130) (> (:y bird) 600))
    (assoc bird :alive false)
    (if (some
          #(intersector! :overlaps (:hit-box bird) (:hit-box %))
          pipes)
      (assoc bird :alive false)
      bird)))

(defn create-game! [screen entities]
  (update! screen :camera (orthographic) :renderer (stage))
  (let [sheet (texture "bird.png")
        ui-skin (skin "fbgame.json")
        big-font (skin! ui-skin :get-font "big-font")
        big-style (style :label big-font (color :white))
        sky (shape  :filled
                    :set-color (color 0.34 0.76 0.77 1)
                    :rect 0 0 420 600)
        background (assoc (texture "sky.png") :x 0 :y 100 :width 420 :height 150)
        land  (assoc (texture "land.png") :x 0 :y 0 :width 400 :height 112 :land? true)
        land2 (assoc (texture "land.png") :x 400 :y 0 :width 400 :height 112 :land? true)
        birds (texture! sheet :split 34 24)
        bird  (assoc (texture (aget birds 1 0))
                :flying-im (texture (aget birds 2 0))
                :normal-im (texture (aget birds 1 0))
                :falling-im (texture (aget birds 0 0))
                :x 150 :y 300 :width 34 :height 24
                :angle 0 :origin-x 0 :origin-y 0 :bird? true :alive true :flying false)
        rand-y (rand-int 200)]


    [sky
     background
     bird
     (for [n (range 2)]
       (let [x (+ 400 (* 200 n))
             y (- rand-y 100)]
         (assoc (texture "pipe.png" :flip false true) :x x :y y :width 46 :height 300 :pipe? true :down? true)))
     (for [n (range 2)]
       (let [x (+ 400 (* 200 n))
             y (+ rand-y 300)]
         (assoc (texture "pipe.png") :x x :y y :width 46 :height 300 :pipe? true :up? true)))
     land
     land2
     (assoc (label "0" big-style) :score 0 :x 160 :y 500 :score? true)]))


(defscreen main-screen
  :on-show
  (fn [screen entities]
;;     (add-timer! screen :spawn-pipe 1 1)
    (create-game! screen entities))
  :on-resize
  (fn [screen entities]
    (height! screen 600))

  :on-render
  (fn [screen entities]
    (clear!)
;;     (println @game-state)
    (swap! game-state #(assoc %1 :elapsed-time (+ (:elapsed-time %1) (:delta-time screen))))
    (let [shared-rand (rand-int 200)]
;;       (clojure.pprint/pprint time-based-rand)
      (some->> (->> entities
                    (map
                      (fn [entity]
                        (->
                          (cond
                            (:bird? entity) (-> entity
                                                (gravity)
                                                (rotate)
                                                (check-still-flying)
                                                (fly)
                                                (animate)
                                                (detect-colision
                                                  (filter #(and (:pipe? %1) (:hit-box %1)) entities))
                                                (reset-when-dead))
                            (:pipe? entity) (if (> (:x entity) -50)
                                              (assoc entity :x (- (:x entity) 3))
                                              (recycle-pipe shared-rand entity))
                            (:land? entity) (if (> (:x entity) -400)
                                              (assoc entity :x (- (:x entity) 3))
                                              (assoc entity :x 365))

                            :else entity)
                          (update-hit-box))))
  ;;                (add-hit-boxes)
                 (render! screen)))))

  :on-key-down
  (fn [screen entities]
    (cond
      (= (:key screen) (key-code :r))
      (reset-game!)
      (= (:key screen) (key-code :w))
      (do
        (map #(if (:bird? %)
                (assoc % :flying true :angle 27)
                %)
             entities))))
  :on-touch-down
  (fn [screen entities]
      (map #(if (:bird? %)
              (assoc % :flying true :angle 27)
              %)
           entities)))

(defscreen start-screen
  :on-show
  (fn [screen entities]
    (create-game! screen entities))
  :on-resize
  (fn [screen entities]
    (height! screen 600))
  :on-render
  (fn [screen entities]
    (clear!)
    (some->> (->> entities
                  (map
                    (fn [entity]
                      (->
                        (cond
                          (:bird? entity) (-> entity)

                          (:land? entity) (if (> (:x entity) -400)
                                            (assoc entity :x (- (:x entity) 3))
                                            (assoc entity :x 365))
                          :else entity))))
               (render! screen))))
  :on-key-down
  (fn [screen entities]
    (start-game!))
  :on-touch-down
  (fn [screen entities]
    (start-game!)))

(defscreen text-screen
  :on-show
  (fn [screen entities]
    (update! screen :camera (orthographic) :renderer (stage))
    (assoc (label "0" (color :white))
           :id :fps
           :x 5))

  :on-render
  (fn [screen entities]
    (->> (for [entity entities]
           (case (:id entity)
             :fps (doto entity (label! :set-text (str (game :fps))))
             entity))
         (render! screen)))

  :on-resize
  (fn [screen entities]
    (height! screen 300)))


(defscreen blank-screen
  :on-render
  (fn [screen entities]
    (clear!))
  :on-key-down
  (fn [screen entities]
    (cond
      (= (:key screen) (key-code :r))
      (app! :post-runnable #(set-screen! flappy-bird-game start-screen text-screen)))))



(set-screen-wrapper!
  (fn [screen screen-fn]
    (try (screen-fn)
      (catch Exception e
        (.printStackTrace e)
        (set-screen! flappy-bird-game blank-screen text-screen)))))


(defgame flappy-bird-game
  :on-create
  (fn [this]
    (set-screen! this start-screen text-screen)))

