(ns flappy-bird.core.desktop-launcher
  (:require [flappy-bird.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. flappy-bird-game "flappy-bird" 450 800)
  (Keyboard/enableRepeatEvents true))

(-main)
