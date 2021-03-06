(ns meins.ui.elements.mapbox
  (:require ["@react-native-mapbox-gl/maps" :as mapbox-gl]
            ["react-native-build-config" :as build-cfg]
            [meins.ui.shared :refer [view]]
            [reagent.core :as r]))

(def mapbox-style-url (js->clj (aget mapbox-gl "default" "StyleURL") :keywordize-keys true))
(def map-view (r/adapt-react-class (aget mapbox-gl "default" "MapView")))
(def point-annotation (r/adapt-react-class (aget mapbox-gl "default" "PointAnnotation")))
(def camera (r/adapt-react-class (aget mapbox-gl "default" "Camera")))
(def set-token (aget mapbox-gl "default" "setAccessToken"))

(defn map-elem [entry]
  (let [{:keys [latitude longitude]} entry
        mapbox-token (aget build-cfg "default" "MGLMapboxAccessToken")]
    (set-token mapbox-token)
    (when (and latitude longitude)
      [map-view {:scrollEnabled false
                 :rotateEnabled false
                 :styleURL      (get mapbox-style-url :Street)
                 :style         {:width         "100%"
                                 :height        250
                                 :margin-bottom 30}}
       [camera {:centerCoordinate [longitude latitude]
                :zoomLevel        15}]
       [point-annotation {:coordinate [longitude latitude]
                          :id         (str (:timestamp entry))}
        [view {:style {:width           24
                       :height          24
                       :alignItems      "center"
                       :justifyContent  "center"
                       :backgroundColor "white"
                       :borderRadius    12}}
         [view {:style {:width           24
                        :height          24
                        :backgroundColor "orange"
                        :borderRadius    12
                        :transform       [{:scale 0.7}]}}]]]])))
