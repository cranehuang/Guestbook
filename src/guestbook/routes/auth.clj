(ns guestbook.routes.auth
  (:require [compojure.core :refer [defroutes GET POST]]
            [guestbook.views.layout :as layout]
            [hiccup.form :refer
             [form-to label text-field password-field submit-button]]
            [noir.response :refer [redirect]]
            [noir.session :as session]
            [noir.validation :refer
             [rule errors? has-value? on-error]]
            [noir.util.crypt :as crypt]
            [guestbook.models.db :as db]))

(defn format-error
  [[error]]
  [:p.error error])

(defn control
  [field name text]
  (list
   (on-error name format-error)
   (label name text)
        (field name)
        [:br]))

(defn registration-page
  [& [error]]
  (layout/common
   (form-to [:post "/register"]
            (control text-field :id "screen name")
            (control password-field :pass "password")
            (control password-field :pass1 "retype password")
            (submit-button "Create Account"))))

(defn handle-registration
  [id pass pass1]
  (rule (has-value? id)
        [:id "Username cannot be empty"])
  (rule (nil? (db/get-user id))
        [:id "The username has being used , change to another"])
  (rule (has-value? pass)
        [:pass "The password is required"])
  (rule (= pass pass1)
        [:pass "the password was not retyped correctly"])
  (if (errors? :id :pass)
    (registration-page)
    (do
      (db/add-user-record {:id id :pass (crypt/encrypt pass)})
      (redirect "/login"))))

(defn login-page
  [& [error]]
  (layout/common
   (form-to [:post "/login"]
            (control text-field :id "screen name")
            (control password-field :pass "password")
            (submit-button "Login"))))

;; (defn handle-login
;;   [id pass]
;;   (cond
;;     (empty? id) (login-page "screen name is required")
;;     (empty? pass) (login-page "password cannot be empty")
;;     (and (= "foo" id) (= "bar" pass))
;;     (do (session/put! :user id)
;;         (redirect "/"))
;;     :else (login-page "Authentication failed")))
(defn handle-login
  [id pass]
  (let [user (db/get-user id)]
    (rule (has-value? id)
          [:id "Screen name is required"])
    (rule (has-value? pass)
          [:pass "Password is required"])
    (rule (and user (crypt/compare pass (:pass user)))
          [:pass "the user doesn't exist or the password is not correctly"])
    (if (errors? :id :pass)
      (login-page)
      (do (session/put! :user id)
          (redirect "/")))))

(defroutes auth-routes
  (GET "/register" [_] (registration-page))
  (POST "/register" [id pass pass1]
          (handle-registration id pass pass1))

  (GET "/login" [] (login-page))
  (POST "/login" [id pass]
          (handle-login id pass))

  (GET "/logout" []
       (layout/common
        (form-to [:post "/logout"]
                 (submit-button "Logout"))))
  (POST "/logout"
        []
        (session/clear!)
        (redirect "/"))
  (GET "/records"
       []
       (noir.response/content-type "text/plain" "The world is going better"))
  (GET "/json" []
       (noir.response/json {:message "Nanan , I love you"})))

;; (defn registration-page
;;   []
;;   (layout/common
;;    (form-to [:post "/register"]
;;             (label "id" "screen name")
;;             (text-field "id")
;;             [:br]
;;             (label "pass" "password")
;;             (password-field "pass")
;;             [:br]
;;             (label "pass1" "retype password")
;;             (password-field "pass1")
;;             [:br]
;;             (submit-button "create account"))))





