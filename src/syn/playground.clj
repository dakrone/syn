(ns syn.core
  (:require [lamina.core :refer :all]
            [lamina.executor :refer :all]
            [slingshot.slingshot :refer :all])
  (:import (java.net URL)))

;;(def r {:status 200 :body "success"})
(def r {:status 300 :body "redirect" :headers {"location" "/foobar"}})

(defn client
  [request]
  (println "performing request:" request)
  (println "returning:" r)
  r)

;; first, sync style

(defn wrap-increment-status
  [client]
  (fn [request]
    (let [new-req (assoc request :incd true)
          resp (client new-req)]
      (update-in resp [:status] inc))))

(def new-client (wrap-increment-status client))

;; same thing, but with async

(defn a-wrap-increment-status
  [client]
  (fn [request]
    (let [p (pipeline
             (fn [resp]
               (update-in resp [:status] inc)))
          new-req (assoc request :incd true)]
      (p (task (client new-req))))))

(def a-new-client (a-wrap-increment-status client))

;; let's try a more difficult middleware

(defn redirect?
  [{:keys [status]}]
  (<= 300 status 399))

(declare a-wrap-redirects)

(defn follow-redirect
  [client {:keys [uri url scheme server-name server-port] :as req}
   {:keys [trace-redirects] :as resp}]
  (println :following-redirects)
  (let [url (or url (str (name scheme) "://" server-name
                         (when server-port (str ":" server-port))
                         uri))
        raw-redirect (get-in resp [:headers "location"])
        redirect (str (URL. (URL. url) raw-redirect))]
    ((a-wrap-redirects client) (-> req
                                   (dissoc :query-params)
                                   (assoc :url redirect
                                          :trace-redirects trace-redirects)))))

(defn a-wrap-redirects
  [client]
  (fn [{:keys [request-method follow-redirects max-redirects
               redirects-count trace-redirects url force-redirects
               throw-exceptions]
        :or {redirects-count 1 trace-redirects [] max-redirects 20}
        :as req}]
    (let [p (pipeline
             (fn [{:keys [status] :as resp}]
               (let [resp-r (assoc resp :trace-redirects
                                   (conj trace-redirects url))]
                 (println :awr-resp resp-r)
                 (cond
                  (= false follow-redirects)
                  resp
                  (not (redirect? resp-r))
                  resp-r
                  (and max-redirects (> redirects-count max-redirects))
                  (if throw-exceptions
                    (throw+ resp-r "Too many redirects: %s" redirects-count)
                    resp-r)
                  (= 303 status)
                  (follow-redirect client (assoc req :request-method :get
                                                 :redirects-count
                                                 (inc redirects-count))
                                   resp-r)
                  (#{301 302 307} status)
                  (do
                    (println "was 301+" status)
                    (cond
                     (#{:get :head} request-method)
                     (follow-redirect client (assoc req :redirects-count
                                                    (inc redirects-count)) resp-r)
                     force-redirects
                     (follow-redirect client (assoc req
                                               :request-method :get
                                               :redirects-count
                                               (inc redirects-count))
                                      resp-r)
                     :else
                     resp-r))
                  :else
                  resp-r))))]
      (p (task (client req))))))

(def a-new-client2 (a-wrap-redirects (a-wrap-increment-status client)))
;;(a-new-client2 {:foo :bar :request-method :get :scheme "http" :server-name "localhost" :uri "/"})
