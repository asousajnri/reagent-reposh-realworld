(ns app.routes
  (:require [reagent.core :as r]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.frontend.controllers :as rfc]
            [reitit.coercion.spec :as rss]
            ;; state
            [app.auth :as auth]
            [app.profile :as profile]
            [app.api :refer [get-token]]
            [app.articles :as articles]
            [app.tags :as tags]
            [app.comments :as comments]
            ;; pages
            [app.pages.home :refer [home-page]]
            [app.pages.login :refer [login-page]]
            [app.pages.register :refer [register-page]]
            [app.pages.settings :refer [settings-page]]
            [app.pages.profile :refer [profile-page]]
            [app.pages.new-article :refer [new-article-page]]
            [app.pages.edit-article :refer [edit-article-page]]
            [app.pages.article-details :refer [article-details-page]]))

(defonce routes-state (r/atom nil))

(defonce temp (atom nil))

(def routes
  [["/"         {:name :routes/home
                 :view #'home-page
                 :controllers [{:start (fn []
                                         (tags/tags-browse)
                                         (if (get-token)
                                           (do
                                             (reset! articles/tab-state :feed)
                                             (articles/articles-feed))
                                           (do
                                             (reset! articles/tab-state :all)
                                             (articles/articles-browse))))
                                :stop (fn []
                                        (tags/reset-tags))}]}]
   ["/login"    {:name :routes/login
                 :view #'login-page
                 :controllers [{:start #(js/console.log "enter - login page")
                                :stop #(js/console.log  "exit - login page")}]}]
   ["/register" {:name :routes/register
                 :view #'register-page
                 :controllers [{:start #(js/console.log "enter - register page")
                                :stop (fn []
                                        (js/console.log "leave - register page")
                                        (when (seq @auth/error-state)
                                          (reset! auth/error-state nil)))}]}]
   ["/settings" {:name :routes/settings
                 :view #'settings-page}]
   ["/editor/new" {:name :routes/new-article
                   :view #'new-article-page}]
   ["/editor/edit/:slug" {:name :routes/edit-article
                          :view #'edit-article-page
                          :parameters
                          {:path {:slug string?}}
                          :controllers
                          [{:params (fn [match]
                                      (:path (:parameters match)))
                            :start (fn [{:keys [slug]}]
                                     (articles/fetch slug))
                            :stop (fn []
                                    (reset! articles/current-article-state nil))}]}]
   ["/article/:slug" {:name :routes/article
                      :view #'article-details-page
                      :parameters
                      {:path {:slug string?}}
                      :controllers
                      [{:params (fn [match]
                                  (:path (:parameters match)))
                        :start (fn [{:keys [slug]}]
                                 (articles/fetch slug)
                                 (comments/browse-comments slug))
                        :stop (fn []
                                (reset! articles/current-article-state nil)
                                (reset! comments/comments-state nil))}]}]
   ["/user/@:username" {:name :routes/profile
                        :view #'profile-page
                        :parameters
                        {:path {:username string?}}
                        :controllers
                        [{:params (fn [match]
                                    (:path (:parameters match)))
                          :start (fn [{:keys [username] :as props}]
                                   (profile/fetch! username)
                                   (articles/fetch-by username)
                                   (println "Entering Profile of - " username)
                                   (reset! temp props))
                          :stop (fn []
                                  (reset! profile/tab-state :author)
                                  (reset! profile/profile-state nil))}]}]])

(defn router-start! []
  (rfe/start!
   (rf/router routes {:data {:coercion rss/coercion
                             :controllers [{:start (fn [] (auth/me))
                                            :stop #(println "Root controller stop")}]}})
   (fn [new-match] (swap! routes-state (fn [old-match]
                                         (if new-match
                                           (assoc new-match :controllers (rfc/apply-controllers (:controllers old-match) new-match))))))
    ;; set to false to enable HistoryAPI
   {:use-fragment false}))
