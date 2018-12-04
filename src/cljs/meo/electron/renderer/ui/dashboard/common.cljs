(ns meo.electron.renderer.ui.dashboard.common
  (:require [moment]
            [re-frame.core :refer [subscribe]]
            [meo.electron.renderer.helpers :as h]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [taoensso.timbre :refer-macros [info debug]]
            [clojure.string :as s]
            [meo.electron.renderer.ui.charts.common :as cc]))

(def month-day "DD.MM.")
(def ymd "YYYY-MM-DD")
(def weekday "ddd")
(defn df [ts format] (.format (moment ts) format))

(def tz-offset
  (-> (js/Date.)
      (.getTimezoneOffset)
      (* 60 1000)))

(defn tick
  "Renders individual timeline tick."
  [pos color w y1 y2]
  [:line {:x1           pos
          :y1           y1
          :x2           pos
          :y2           y2
          :stroke       color
          :stroke-width w}])

(defn scatter-chart [scores point-mapper color]
  (let [points (map-indexed point-mapper scores)]
    [:g
     (for [p points]
       ^{:key (str p)}
       [:circle {:cx    (:x p)
                 :cy    (:y p)
                 :r     3.2
                 :style {:stroke  color
                         :fill    color
                         :opacity 0.5}}])]))

(defn line [y s w]
  [:line {:x1           195
          :x2           2000
          :y1           y
          :y2           y
          :stroke-width w
          :stroke       s}])

(defn rect [{:keys []}]
  (let []
    (fn [{:keys [v x w y h cls ymd color label local]}]
      (let [display-text [:span ymd ": " [:strong v] " " label]
            enter #(swap! local assoc :display-text display-text)
            leave #(swap! local assoc :display-text "")]
        [:g
         [:rect {:on-mouse-enter enter
                 :on-mouse-leave leave
                 :x              x
                 :y              (- y h)
                 :width          w
                 :height         h
                 :fill           color
                 :class          (cc/weekend-class cls {:date_string ymd})}]
         (when (:show-label @local)
           [:text {:x           (+ x 11)
                   :y           (- y 5)
                   :font-size   8
                   :fill        "#777"
                   :text-anchor "middle"}
            v])]))))

(defn indexed-days [stats tag k start days]
  (let [d (* 24 60 60 1000)
        rng (range (inc days))
        indexed (map-indexed (fn [n v]
                               (let [offset (* n d)
                                     ts (+ start offset)
                                     ymd (df ts ymd)
                                     v (get-in stats [ymd tag k] 0)
                                     weekday (df ts weekday)]
                                 [n {:ymd     ymd
                                     :v       v
                                     :weekday weekday}]))
                             rng)]
    indexed))

(defn row-label [label y h]
  [:text {:x           180
          :y           (+ y (+ 5 (/ h 2)))
          :font-size   20
          :fill        "#777"
          :text-anchor "end"}
   label])

(defn barchart-row [_ _]
  (let [gql-res (subscribe [:gql-res])
        backend-cfg (subscribe [:backend-cfg])
        custom-fields (reaction (:custom-fields @backend-cfg))]
    (fn barchart-row [{:keys [days span mx tag h y field color local
                              cls threshold success-cls] :as m} put-fn]
      (when (and tag field (seq tag))
        (let [btm-y (+ y h)
              qid (keyword (s/replace (subs (str tag) 1) "-" "_"))
              data (get-in @gql-res [:dashboard :data qid])
              indexed (map-indexed (fn [i x] [i x]) data)
              label (get-in @custom-fields [tag :fields (keyword field) :label])
              mx (or mx
                     (apply max (map
                                  (fn [x]
                                    (:value
                                      (first (filter #(= (name field) (:field %))
                                                     (:fields x)))
                                      0))
                                  data)))
              scale (if (pos? mx) (/ (- h 3) mx) 1)]
          [:g
           [row-label (or label tag) y h]
           (for [[n {:keys [date_string fields]}] indexed]
             (let [field (first (filter #(= (name field) (:field %)) fields))
                   v (:value field 0)
                   d (* 24 60 60 1000)
                   offset (* n d)
                   span (if (zero? span) 1 span)
                   scaled (* 1800 (/ offset span))
                   x (+ 201 scaled)
                   v (min mx v)
                   h (* v scale)
                   cls (if (and threshold (> v threshold))
                         success-cls
                         cls)
                   display-v (if (= :duration field)
                               (h/m-to-hh-mm v)
                               v)]
               ^{:key (str tag field n)}
               [rect {:v     display-v
                      :x     x
                      :w     (/ 1500 days)
                      :ymd   date_string
                      :y     btm-y
                      :h     h
                      :label label
                      :local local
                      :tag   tag
                      :cls   cls
                      :color color
                      :n     n}]))
           [line (+ y h) "#000" 2]])))))

(defn points-by-day-chart [{:keys [y h label]}]
  (let [gql-res (subscribe [:gql-res])]
    (fn points-by-day-render [{:keys [y h label days span]}]
      (let [data (get-in @gql-res [:dashboard :data :award-points])
            btm-y (+ y h)
            by-day (map (fn [m] [(:date_string m) m]) (:by-day data))
            daily-totals (map (fn [[d v]] (h/add (:task v) (:habit v))) by-day)
            max-val (apply max daily-totals)
            w (dec (/ 1400 days))
            indexed (map-indexed (fn [idx [day v]] [idx [day v]]) by-day)]
        [:g
         (for [[idx [day v]] indexed]
           (let [v (h/add (:task v) (:habit v))
                 y-scale (/ h (or max-val 1))
                 h (if (pos? v) (* y-scale v) 0)
                 d (* 24 60 60 1000)
                 offset (* idx d)
                 span (if (zero? span) 1 span)
                 scaled (* 1800 (/ offset span))
                 x (+ 201 scaled)]
             (when (pos? max-val)
               ^{:key (str day idx)}
               [:rect {:x      x
                       :y      (- btm-y h)
                       :fill   "#7FE283"
                       :width  w
                       :height h}])))
         (for [[idx [day v]] indexed]
           (let [v (:task v)
                 y-scale (/ h (or max-val 1))
                 h (if (pos? v) (* y-scale v) 0)
                 d (* 24 60 60 1000)
                 offset (* idx d)
                 span (if (zero? span) 1 span)
                 scaled (* 1800 (/ offset span))
                 x (+ 201 scaled)]
             (when (pos? max-val)
               ^{:key (str day idx)}
               [:rect {:x      x
                       :y      (- btm-y h)
                       :fill   "#42b8dd"
                       :width  w
                       :height h}])))
         [line (+ y h) "#000" 2]
         [row-label label y h]]))))

(defn points-lost-by-day-chart [{:keys [y h label]}]
  (let [stats (subscribe [:stats])]
    (fn points-by-day-render [{:keys [y h label]}]
      (let [btm-y (+ y h)
            award-points (:award-points @stats)
            by-day (sort-by first (:by-day-skipped award-points))
            daily-totals (map (fn [[d v]] (:habit v)) by-day)
            max-val (apply max daily-totals)
            indexed (map-indexed (fn [idx [day v]] [idx [day v]])
                                 (take-last 180 by-day))]
        [:g
         [line (+ y h) "#000" 2]
         [row-label label y h]]))))
