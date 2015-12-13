(ns quarter.gear)

;; Normalize gear by expanding shorthand
(defn expand-gear [gear]
  (for [[category & section] gear]
    [category
     (for [gear_desc section
           :let [[id desc & count] (if (vector? gear_desc) gear_desc [gear_desc (name gear_desc) 1])]]
       {:gear_id id :description desc :category category :count (or (first count) 1)})]))

(def default-people ["Matt Farago" "Michael Sullivan" "Ken Vogel"])

(def gear-types
  {:patrol-box
   (expand-gear [["Camping Gear"
                  [:lantern "Propane lantern + case"]
                  [:gloves "Cowhide work gloves"]
                  [:water-jug "Five gallon, collapsable water container"]]
                 ["Cooking Gear"
                  [:dual-stove "Dual burner propane stove"]
                  [:single-stove "Single burner propane stove"]
                  [:griddle "Large griddle"]
                  :cutting-board
                  :cocoa-pot
                  ]
                 ["Cooking Utensil Box"
                  :spatula
                  :ladle
                  :slotted-spoon
                  :can-opener
                  :large-knife
                  :small-knife
                  [:plasticware "Plastic knife, fork, spoon"]
                  [:lighter "Butane stove lighter"]
                  ]
                 ["Patrol cook kit"
                  :large-pot
                  :medium-pot
                  [:large-pan "Large frying pan"]
                  [:small-pan "Small frying pan"]
                  [:plates "plates" 4]
                  ]
                 ["Cleaning gear"
                  [:wash-basins "Wash basins" 3]
                  [:hand-sanitizer "Hand sanitizer (Germex)"]
                  [:sponges "Sponges" 2]
                  [:rubber-gloves "Pair of rubber gloves"]
                  ]
                 ["Miscellaneous"
                  [:trash-bags "Box of trash bags"]
                  [:utility-line "50 yards of utility line"]
                  [:duct-tape "Roll of duct tape"]
                  [:small-propane "Small propane canisters" 3]
                  ]])})

(def gear-sites {"Patrol Box 3" :patrol-box "Patrol Box 5" :patrol-box "Patrol Box 6" :patrol-box})
