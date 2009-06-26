(ns clj-record.associations
  (:use clj-record.util))


(defn expand-init-option
  "Called via init-model when an :associations option group is encountered."
  [model-name association-type-sym association-name & options]
  (let [assoc-fn (ns-resolve 'clj-record.associations association-type-sym)]
    (apply assoc-fn model-name association-name options)))

(defn has-many
  "Called indirectly via clj-record.core/init-model.
  Defines an association to a model whose name is infered by singularizing association-name.
  In ns foo's init-model, (has-many bars) will define find-bars and destroy-bars functions in foo,
  each of which take a foo record and find or destroy bars by {:foo_id (record :id)}."
  [model-name association-name & options]
  (let [option-map (apply hash-map options)
        associated-model-name (singularize (name association-name))
        assoc-name (if (:as option-map)
                     (name (:as option-map))
                     association-name)
        foreign-key-attribute (keyword (if (:on option-map)
                                         (name (:on option-map))
                                         (str model-name "_id")))
        find-fn-name (symbol (str "find-" assoc-name))
        destroy-fn-name (symbol (str "destroy-" assoc-name))]
    `(do
      (defn ~find-fn-name [record#]
        (clj-record.core/find-records ~associated-model-name {~foreign-key-attribute (record# :id)}))
      (defn ~destroy-fn-name [record#]
        (clj-record.core/destroy-records ~associated-model-name {~foreign-key-attribute (record# :id)})))))

(defn belongs-to
  "Called indirectly via clj-record.core/init-model.
  Defines an association to a model named association-name.
  In ns bar's init-model, (belongs-to foo) will define find-foo in bar."
  [model-name association-name & options]
  (let [option-map (apply hash-map options)
        associated-model-name (singularize (name association-name))
        find-fn-name (symbol (str "get-" (if (:as option-map)
                                           (name (:as option-map))
                                           (singularize (name association-name)))))
        foreign-key-attribute (keyword (if (:on option-map)
                                         (name (:on option-map))
                                         (str associated-model-name "_id")))]
    `(defn ~find-fn-name [record#]
      (clj-record.core/get-record ~associated-model-name (~foreign-key-attribute record#)))))
