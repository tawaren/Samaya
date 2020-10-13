package samaya.structure

import samaya.structure.types.Hash
import samaya.types.Location

class LinkablePackage(
   val interfacesOnly:Boolean,
   val location: Location,
   val hash:Hash,
   override val name: String,
   override val components: Seq[Interface[Component]],
   override val dependencies: Seq[LinkablePackage],
) extends Package {
}

