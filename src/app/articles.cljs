(ns app.articles
  (:require [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [app.api :refer [api-uri get-auth-header]]
            [ajax.core :refer [GET POST PUT DELETE json-request-format json-response-format]]))

(defonce articles-state (r/atom nil))
(defonce current-article-state (r/atom nil))
(defonce tab-state (r/atom :all))
(defonce tag-state (r/atom nil))
(defonce loading-state (r/atom false))
(defonce submitting-state (r/atom false))
(defonce error-state (r/atom nil))

(defn handler [response]
  (reset! loading-state false)
  (reset! articles-state response))

(defn error-handler [{:keys [status status-text]}]
  (reset! loading-state false)
  (.log js/console (str "something bad happened: " status " " status-text)))

(defn articles-browse []
  (reset! loading-state true)
  (GET (str api-uri "/articles?limit=10&offset=0")
       {:handler handler
        :headers (get-auth-header)
        :response-format (json-response-format {:keywords? true})
        :error-handler error-handler}))

(defn limit [total page]
  (str "limit=" total "&offset=" (or (* page total) 0)))

(defn articles-feed []
  (reset! loading-state true)
  (GET (str api-uri "/articles/feed?limit=10&offset=0")
       {:handler handler
        :headers (get-auth-header)
        :response-format (json-response-format {:keywords? true})
        :error-handler error-handler}))

(defn fetch-by
  ([author] (fetch-by author 0))
  ([author page]
   (reset! loading-state true)
   (GET (str api-uri "/articles?author=" (js/encodeURIComponent author) "&" (limit 5 page))
        {:handler handler
         :headers (get-auth-header)
         :response-format (json-response-format {:keywords? true})
         :error-handler error-handler})))

(defn favourited-by [author page]
  (reset! loading-state true)
  (GET (str api-uri "/articles?favorited=" (js/encodeURIComponent author) "&" (limit 5 page))
       {:handler handler
        :headers (get-auth-header)
        :response-format (json-response-format {:keywords? true})
        :error-handler error-handler}))

(defn articles-by-tag [tag]
  (reset! loading-state true)
  (GET (str api-uri "/articles?" (limit 10 0) "&tag=" tag)
       {:handler handler
        :error-handler error-handler
        :headers (get-auth-header)
        :response-format (json-response-format {:keywords? true})}))

(defn create-success! [{:keys [article]}]
  (reset! submitting-state false)
  (reset! current-article-state article)
  (rfe/push-state :routes/article {:slug (:slug article)}))

(defn create-error! [{{:keys [errors]} :response}]
  (reset! submitting-state false)
  (reset! error-state errors))

(defn create-article! [article]
  (reset! submitting-state true)
  (POST (str api-uri "/articles")
        {:params {:article article}
         :handler create-success!
         :error-handler create-error!
         :headers (get-auth-header)
         :format (json-request-format)
         :response-format (json-response-format {:keywords? true})}))

;; fetch
(defn fetch-success! [{:keys [article]}]
  (reset! loading-state false)
  (reset! current-article-state article))

(defn fetch [slug]
  (reset! loading-state true)
  (GET (str api-uri "/articles/" slug)
       {:handler fetch-success!
        :error-handler error-handler
        :headers (get-auth-header)
        :response-format (json-response-format {:keywords? true})}))

(defn delete-success! [resp]
  (rfe/push-state :routes/home))

(defn delete-article! [slug]
  (reset! loading-state true)
  (DELETE (str api-uri "/articles/" slug)
          {:handler delete-success!
           :error-handler error-handler
           :headers (get-auth-header)
           :response-format (json-response-format {:keywords? true})}))

;; edit
(defn update-article! [article]
  (reset! submitting-state true)
  (PUT (str api-uri "/articles/" (:slug article))
       {:params {:article (dissoc article :slug)}
        :handler create-success!
        :error-handler create-error!
        :headers (get-auth-header)
        :format (json-request-format)
        :response-format (json-response-format {:keywords? true})}))

(defn update-article [article updated-article]
  (if (= (:slug updated-article) (:slug article))
    updated-article
    article))

(defn toggle-favourite-success! [{:keys [article]}]
  (reset! submitting-state false)
  (swap! articles-state assoc :articles (map #(update-article % article) (:articles @articles-state))))

(defn favourite-article! [article]
  (reset! submitting-state true)
  (POST (str api-uri "/articles/" (:slug article) "/favorite")
        {:params {:article (dissoc article :slug)}
         :handler toggle-favourite-success!
         :error-handler create-error!
         :headers (get-auth-header)
         :format (json-request-format)
         :response-format (json-response-format {:keywords? true})}))

(defn unfavourite-article! [article]
  (reset! submitting-state true)
  (DELETE (str api-uri "/articles/" (:slug article) "/favorite")
          {:params {:article (dissoc article :slug)}
           :handler toggle-favourite-success!
           :error-handler create-error!
           :headers (get-auth-header)
           :format (json-request-format)
           :response-format (json-response-format {:keywords? true})}))
