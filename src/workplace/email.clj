(ns workplace.email
  "Send emails using Exchange. Allegedly will work with Microsoft 365 too.
   https://github.com/OfficeDev/ews-java-api/wiki/Getting-Started-Guide

   Expects config map with :host Exchange uri."
  (:require [clojure.spec.alpha :as s])
  (:import (microsoft.exchange.webservices.data.core.enumeration.misc ExchangeVersion)
           (microsoft.exchange.webservices.data.core ExchangeService)
           (microsoft.exchange.webservices.data.credential WebCredentials)
           (microsoft.exchange.webservices.data.core.service.item EmailMessage)
           (java.net URI)
           (microsoft.exchange.webservices.data.property.complex MessageBody)
           (microsoft.exchange.webservices.data.core.exception.http HttpErrorException)))

(s/def ::host #(re-matches #"https://.+/EWS/Exchange.asmx" %))
(s/def ::config (s/keys :req-un [::host]))

(defn service
  [config email password]
  (doto (ExchangeService. ExchangeVersion/Exchange2010_SP2)
    (.setUrl (URI. (-> config :host)))
    (.setCredentials (WebCredentials. email password))))

(defn send-email!
  [^ExchangeService service subject body recipients]
  {:pre [(instance? ExchangeService service)
         (string? subject)
         (string? body)
         (vector? recipients)]}
  (let [em (doto (EmailMessage. service)
             (.setSubject subject)
             (.setBody (MessageBody/getMessageBodyFromText body)))]
    (doseq [recipient recipients]
      (.add (.getToRecipients em) recipient))
    (try (and (.send em) true) ; TODO look at .sendAndSaveCopy
         (catch HttpErrorException _ false))))