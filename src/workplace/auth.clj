(ns workplace.auth
  "Expects config map with :host endpoint and :domain user domain."
  (:import (com.imperva.ddc.core.query Endpoint QueryRequest ObjectType FieldType Field)
           (com.imperva.ddc.service DirectoryConnectorService)
           (com.imperva.ddc.core.exceptions AuthenticationException)
           (com.imperva.ddc.core.language QueryAssembler PhraseOperator)
           (com.imperva.ddc.core Connector)))

; ps> gpresult /Z
; ps> $env:logonserver

(defn endpoint
  "Use inside with-open."
  [config username password]
  (doto (Endpoint.)
    (.setHost (-> config :host))
    (.setPassword password)
    (.setUserAccountName (str (-> config :domain) \\ username))))

(defn authenticate [config username password]
  (with-open [e (endpoint config username password)]
    (try (-> e DirectoryConnectorService/authenticate .isError not)
         (catch AuthenticationException _ false))))

(defn get-email
  [endpoint employee-number]
  (let [qr (doto (QueryRequest.)
             (.setEndpoints [endpoint])
             (.setObjectType ObjectType/USER)
             (.addRequestedField FieldType/EMAIL)
             #_(.addRequestedField FieldType/TITLE) ; role
             #_(.addRequestedField FieldType/LAST_LOGON)
             #_(.addRequestedField FieldType/DISTINGUISHED_NAME) ; LDAP stuff
             #_(.addRequestedField FieldType/CREATION_TIME)
             #_(.addRequestedField FieldType/FIRST_NAME)
             #_(.addRequestedField FieldType/LAST_NAME)
             #_(.addRequestedField FieldType/DEPARTMENT)
             #_(.addRequestedField FieldType/PHONE_NUMBER)
             #_ (.addRequestedField FieldType/PHOTO)) ; returns bytearray!
        qa (QueryAssembler.)
        qs (->> (str employee-number) (.addPhrase qa FieldType/LOGON_NAME PhraseOperator/EQUAL) .closeSentence)
        _ (.addSearchSentence qr qs)]
    (with-open [qc (Connector. qr)]
      (let [[f & r :as emails] (doall (for [er (.getAll (.execute qc))
                                            ^Field qf (.getValue er)]
                                        (.getValue qf)))]
        (if r emails f)))))