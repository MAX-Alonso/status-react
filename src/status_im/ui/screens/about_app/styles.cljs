(ns status-im.ui.screens.about-app.styles
  (:require [status-im.ui.components.colors :as colors]))

(def peer-view
  {:background-color colors/white
   :padding-bottom 5
   :padding-horizontal 16})

(def peer-text
  {:font-size 10})

(def about-title
  {:background-color colors/white
   :font-size 20
   :padding-vertical 5
   :padding-horizontal 16})

(def about-title-text
  {:font-size 20})

(def learn-more-title
  {:typography :title-bold})

(def learn-more-text
  {:color colors/gray})
