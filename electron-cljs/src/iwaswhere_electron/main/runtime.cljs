(ns iwaswhere-electron.main.runtime
  (:require [path :refer [normalize]]
            [electron :refer [app]]
            [cljs.nodejs :as nodejs :refer [process]]
            [clojure.string :as s]))

(def runtime-info
  (let [user-data (.getPath app "userData")
        cwd (.cwd process)
        rp (.-resourcesPath process)
        app-path (if (s/includes? rp "Electron.app")
                   cwd
                   (str rp "/app"))
        platform (.-platform process)
        jdk (case platform
              "darwin" "/bin/zulu8.23.0.3-jdk8.0.144-mac_x64/bin/java"
              "win32" "/bin/zulu8.23.0.3-jdk8.0.144-win_x64/bin/java"
              "/bin/zulu8.23.0.3-jdk8.0.144-linux_x64/bin/java")
        info {:platform       (.-platform process)
              :java           (str app-path jdk)
              :data-path      (str user-data "/data")
              :cache          (str user-data "/data/cache.dat")
              :jar            (str app-path "/bin/iwaswhere.jar")
              :blink          (str app-path "/bin/blink1-mac-cli")
              :user-data      user-data
              :cwd            cwd
              :pid-file       (str user-data "/iwaswhere.pid")
              :resources-path rp
              :app-path       app-path}]
    (into {} (map (fn [[k v]] [k (normalize v)]) info))))