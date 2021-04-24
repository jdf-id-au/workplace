(ns workplace.email
  "Send emails using Exchange. Allegedly will work with Microsoft 365 too.
   https://github.com/OfficeDev/ews-java-api/wiki/Getting-Started-Guide

   Expects config map with :host Exchange uri."
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log])
  (:import (microsoft.exchange.webservices.data.core.enumeration.misc ExchangeVersion)
           (microsoft.exchange.webservices.data.core ExchangeService)
           (microsoft.exchange.webservices.data.credential WebCredentials)
           (microsoft.exchange.webservices.data.core.service.item EmailMessage)
           (java.net URI)
           (microsoft.exchange.webservices.data.property.complex MessageBody)))

(s/def ::host #(re-matches #"https://.+/EWS/Exchange.asmx" %))
(s/def ::config (s/keys :req-un [::host]))

(defn service
  "Authenticate with Exchange server."
  [{:keys [host]} email password]
  (doto (ExchangeService. ExchangeVersion/Exchange2010_SP2)
    (.setUrl (URI. host))
    (.setCredentials (WebCredentials. email password))))

(defn compose
  "`attachments` should be a collection of [display-name attachment] pairs.
   Supports filename, InputStream or bytes.
   Attachment CIDs are same as display-name for linking.
   Caller should call .save (required for attachments) and .send ."
  [^ExchangeService service subject recipients body attachments]
  {:pre [(instance? ExchangeService service) ; TODO change to spec
         (string? subject)
         (string? body)
         (vector? recipients)
         (vector? attachments)]}
  (let [em (EmailMessage. service)]
    (doseq [^String recipient recipients] (.add (.getToRecipients em) recipient))
    (.setSubject em subject)
    (.setBody em (MessageBody/getMessageBodyFromText body))
    (doseq [[display-name attachment] attachments] ; content-type is detected
      (-> (.getAttachments em)
          (.addFileAttachment display-name attachment)
          (.setContentId display-name)))
    em))