(ns meo.electron.renderer.ui.img.core
  (:require [re-frame.core :refer [subscribe]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.impl.component :as ric]
            [taoensso.timbre :refer [info error debug]]
            [meo.electron.renderer.ui.entry.quill :as q]
            [meo.electron.renderer.ui.entry.utils :as eu]
            [meo.electron.renderer.ui.leaflet :as l]
            [meo.electron.renderer.ui.mapbox :as mb]
            [meo.electron.renderer.helpers :as h]
            [clojure.data.avl :as avl]
            [meo.common.utils.misc :as u]
            [meo.electron.renderer.ui.re-frame.db :refer [emit]]
            [clojure.string :as s]
            [mapbox-gl]
            [turndown :as turndown]
            [markdown.core :as md]
            [reagent.core :as r]
            [clojure.set :as set]
            [clojure.string :as str]))

(defn stars-view [entry local]
  (let [star (fn [idx n]
               (let [click (fn [ev]
                             (let [updated (assoc-in entry [:stars] idx)]
                               (debug "stars click" updated)
                               (swap! local assoc :selected updated)
                               (emit [:entry/update updated])))]
                 [:i.fa-star {:class    (if (<= idx n) "fas" "fal")
                              :on-click click}]))
        stars (:stars entry 0)]
    [:span.stars
     [star 1 stars]
     [star 2 stars]
     [star 3 stars]
     [star 4 stars]
     [star 5 stars]]))

(defn image-view
  "Renders image view. Uses resized and properly rotated image endpoint
   when JPEG file requested."
  [entry locale local]
  (when-let [file (:img_file entry)]
    (let [resized-rotated (h/thumbs-2048 file)]
      [:div.slide
       [:img {:src resized-rotated}]])))

(defn thumb-view [entry selected local]
  (when-let [file (:img_file entry)]
    (let [thumb (h/thumbs-256 file)
          click (fn [_] (swap! local assoc-in [:selected] entry))]
      [:li.thumb
       {:on-click click
        :class    (when (= entry selected) "selected")}
       [:img {:src       thumb
              :draggable false}]])))

(defn stars-filter [local]
  (let [selected (:filter @local)
        cls #(if (contains? selected %) "fas" "fal")
        sel (fn [n]
              (fn [_]
                (swap! local update-in [:filter] #(if (contains? % n)
                                                    (disj % n)
                                                    (conj % n)))
                (swap! local dissoc :selected)))]
    [:div.stars-filter
     [:div {:on-click (sel 5)}
      [:i.fa-star {:class (cls 5)}]
      [:i.fa-star {:class (cls 5)}]
      [:i.fa-star {:class (cls 5)}]
      [:i.fa-star {:class (cls 5)}]
      [:i.fa-star {:class (cls 5)}]]
     [:div {:on-click (sel 4)}
      [:i.fa-star {:class (cls 4)}]
      [:i.fa-star {:class (cls 4)}]
      [:i.fa-star {:class (cls 4)}]
      [:i.fa-star {:class (cls 4)}]]
     [:div {:on-click (sel 3)}
      [:i.fa-star {:class (cls 3)}]
      [:i.fa-star {:class (cls 3)}]
      [:i.fa-star {:class (cls 3)}]]
     [:div {:on-click (sel 2)}
      [:i.fa-star {:class (cls 2)}]
      [:i.fa-star {:class (cls 2)}]]
     [:div {:on-click (sel 1)}
      [:i.fa-star {:class (cls 1)}]]]))

(defn text-editor
  "Quill-based text editor for entry text for use in the image gallery view.
   I chose Quill as opposed to Draft.js to have a comparison, as Draft.js
   has given me more problems than I anticipated. Working mostly fine now,
   but won't hurt to have a potential alternative. Curious about your
   experience with either."
  [entry]
  (let [local (r/atom entry)]
    (fn [entry]
      (let [html (md/md->html (:md @local))
            td (turndown. (clj->js {:headingStyle "atx"}))
            on-change (fn [x html]
                        (let [md (.turndown td html)
                              updated (assoc-in @local [:md] md)]
                          (reset! local updated)
                          (emit [:entry/update-local updated])))
            on-save (fn [_] (emit [:entry/update @local]))]
        [:div.gallery-editor
         [q/editor {:id           :quill-editor
                    :content      html
                    :save-fn      on-save
                    :on-change-fn on-change}]
         [:div
          (when (not= entry @local)
            [:div.save {:on-click on-save}
             [:i.fas.fa-save]
             "save"])]]))))

(defn info-drawer [selected locale]
  (let [local (r/atom {})
        backend-cfg (subscribe [:backend-cfg])]
    (fn [selected locale]
      (let [ts (:timestamp selected)
            file (:img_file selected)
            mapbox-token (:mapbox-token @backend-cfg)
            external (str h/photos file)
            {:keys [latitude longitude]} selected
            original-filename (last (s/split (:img_rel_path selected) #"[/\\\\]"))]
        [:div.info-drawer
         (when (and latitude longitude
                    (not (and (zero? latitude)
                              (zero? longitude))))
           (if mapbox-token
             [mb/mapbox-cls {:local        local
                             :id           (str ts)
                             :selected     selected
                             :mapbox-token mapbox-token
                             :put-fn       emit}]
             [l/leaflet-map selected true {} emit]))
         [:time (h/localize-datetime-full ts locale)]
         [text-editor selected]
         [stars-view selected local]
         [:div.stars
          [:span original-filename]
          [:a {:href external :target "_blank"} [:i.fas.fa-external-link-alt]]]]))))

(defn carousel [_]
  (let [locale (subscribe [:locale])]
    (fn [{:keys [filtered local selected-idx prev-click next-click]}]
      (let [locale @locale
            selected (or (:selected @local)
                         (first filtered))
            n (count filtered)
            two-or-more (< 1 n)]
        [:div
         [:div.carousel.carousel-slider {:style {:width "100%"}}
          [:div.filters
           [stars-filter local]]
          [:div.slider-wrapper.axis-horizontal
           (when two-or-more
             [:button.control-arrow.control-prev {:on-click prev-click}])
           [image-view selected locale local]
           (when two-or-more
             [:button.control-arrow.control-next {:on-click next-click}])]
          [info-drawer selected locale]
          (when two-or-more
            [:p.carousel-status (inc selected-idx) "/" n])]
         [:div.carousel
          [:div.thumbs-wrapper.axis-horizontal
           [:ul
            (for [entry filtered]
              ^{:key (:timestamp entry)}
              [thumb-view entry selected local])]]]]))))

(defn gallery
  "Renders thumbnails of photos in linked entries. Respects private entries."
  [entries]
  (let [local (r/atom {:filter #{}})
        filter-by-stars (fn [entry]
                          (or (empty? (:filter @local))
                              (contains? (:filter @local)
                                         (:stars entry))))
        cmp (fn [a b] (compare (:timestamp a) (:timestamp b)))
        sorted (reaction (sort-by :timestamp @entries))
        avl-sort (fn [xs] (into (avl/sorted-set-by cmp) (filter filter-by-stars xs)))
        selected (reaction (or (:selected @local)
                               (first (vec (avl-sort @sorted)))))
        next-click #(let [avl-sorted (avl-sort @sorted)
                          slide (avl/nearest avl-sorted > @selected)]
                      (swap! local assoc-in [:selected] (or slide
                                                            (first (vec avl-sorted)))))
        prev-click #(let [avl-sorted (avl-sort @sorted)
                          slide (avl/nearest avl-sorted < @selected)]
                      (swap! local assoc-in [:selected] (or slide
                                                            (last (vec avl-sorted)))))
        keydown (fn [ev]
                  (let [key-code (.. ev -keyCode)
                        meta-key (.-metaKey ev)
                        set-stars (fn [n]
                                    (let [selected @selected
                                          updated (assoc-in selected [:stars] n)]
                                      (debug updated)
                                      (emit [:entry/update updated])))]
                    (info key-code meta-key)
                    (when (= key-code 37) (prev-click))
                    (when (= key-code 39) (next-click))
                    (when (and meta-key (= key-code 49)) (set-stars 1))
                    (when (and meta-key (= key-code 50)) (set-stars 2))
                    (when (and meta-key (= key-code 51)) (set-stars 3))
                    (when (and meta-key (= key-code 52)) (set-stars 4))
                    (when (and meta-key (= key-code 53)) (set-stars 5))
                    (.stopPropagation ev)))
        stop-watch #(.removeEventListener js/document "keydown" keydown)
        start-watch #(do (.addEventListener js/document "keydown" keydown)
                         (js/setTimeout stop-watch 60000))]
    (fn gallery-render [entries]
      (let [sorted-filtered (filter filter-by-stars @sorted)
            selected-idx (avl/rank-of (avl-sort sorted-filtered) @selected)]
        [:div.gallery.page.fullscreen {:on-mouse-enter start-watch
                                       :on-mouse-over  start-watch
                                       :on-mouse-leave stop-watch}
         [carousel {:filtered     sorted-filtered
                    :local        local
                    :selected-idx selected-idx
                    :next-click   next-click
                    :prev-click   prev-click}]]))))

(defn gallery-entries [entry]
  (filter :img_file (concat [entry]
                            (:comments entry)
                            (:linked entry))))

(defn gallery-page []
  (let [res (subscribe [:gql-res2])
        entries (reaction (gallery-entries (first (vals (get-in @res [:gallery :res])))))]
    (fn []
      [:div.flex-container
       [gallery entries]])))