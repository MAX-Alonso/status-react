(ns status-im.utils.snoopy
  (:require [status-im.react-native.js-dependencies :as js-dependencies]
            [status-im.utils.config :as config]
            ["react-native/Libraries/vendor/emitter/EventEmitter" :refer [EventEmitter]]
            ["rn-snoopy" :default snoopy]
            ["rn-snoopy/stream/filter" :default sn-filter]
            ["rn-snoopy/stream/bars" :default bars]
            ["rn-snoopy/stream/buffer" :default buffer]))

(defn create-filter [f]
  (fn [message]
    (let [method    (.-method message)
          module    (.-module message)
          args      (.-args message)
          first-arg (when (pos? (.-length args))
                      (aget args 0))]
      (f {:method    method
          :module    module
          :first-arg first-arg}))))

(defn status-module-filter [{:keys [method module first-arg]}]
  (or (= module "Status")
      (and (= module "RCTNativeAppEventEmitter")
           (= method "emit")
           (= first-arg "gethEvent"))
      (and
       (string? method)
       (clojure.string/starts-with? method "<callback for Status."))))

(defn timer-filter [{:keys [method]}]
  (contains? #{"callTimers" "createTimer"} method))

(defn websocket-filter [{:keys [module first-arg]}]
  (or (= module "WebSocketModule")
      (and (= module "RCTDeviceEventEmitter")
           (contains? #{"websocketFailed" "websocketMessage"} first-arg))))

(defn ui-manager-filter [{:keys [module]}]
  (= module "UIManager"))

(defn touches-filter [{:keys [method module]}]
  (and (= module "RCTEventEmitter")
       (= method "receiveTouches")))

(defn native-animation-filter [{:keys [method module]}]
  (or (= module "NativeAnimatedModule")
      (and
       (string? method)
       (clojure.string/starts-with? method "<callback for NativeAnimatedModule."))))

(defn keyboard-observer-filter [{:keys [module]}]
  ;; mostly all calls to KeyboardObserver are done by FlatList
  (= module "KeyboardObserver"))

(defn threshold-warnings
  [{:keys [filter-fn label tick? print-events? threshold events threshold-message]}]
  (.subscribe ((bars
                (fn [a] (.-length a))
                threshold
                tick?
                true
                label
                threshold-message)
               ((buffer) ((sn-filter (create-filter filter-fn)
                                     print-events?)
                          events)))))

;; In order to enable snoopy set SNOOPY=1 in .env file.
;; By default events are not printed and you will see warnings only when
;; the number of events is exceeding the threshold.
;; For debugging UI perf, in particular the number of bridge calls caused
;; by view components set `:print-events?=true` for `ui-manager-filter`,
;; and then collect printed data in logs.
(defn subscribe! []
  (when config/snoopy-enabled?
    (let [emitter-class EventEmitter
          emitter       (emitter-class.)
          events        (.stream snoopy emitter)]
      (threshold-warnings
       {:filter-fn         (constantly true)
        :label             "all messages"
        :threshold-message "too many calls to bridge, something suspicious is happening"
        :tick?             false
        :print-events?     false
        :threshold         400
        :events            events})

      (threshold-warnings
       {:filter-fn         timer-filter
        :label             "timer"
        :threshold-message "too many setTimeout/setInterval calls"
        :tick?             false
        :print-events?     false
        :threshold         70
        :events            events})

      (threshold-warnings
       {:filter-fn         ui-manager-filter
        :label             "timer"
        :threshold-message (str "too many calls to UIManager, most likely during navigation. "
                                "Please consider preloading of screens or lazy loading of some components")
        :tick?             false
        :print-events?     true
        ;; todo(rasom): revisit this number when/if
        ;; https://github.com/status-im/status-react/pull/2849 will be merged
        :threshold         200
        :events            events}))))
