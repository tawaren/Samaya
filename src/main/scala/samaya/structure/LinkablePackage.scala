package samaya.structure

import samaya.plugin.service.PackageEncoder
import samaya.structure.types.Hash
import samaya.types.{ContentAddressable, Directory, Identifier}

class LinkablePackage(
                       val interfacesOnly:Boolean,
                       val location: Directory,
                       val hash:Hash,
                       override val name: String,
                       override val components: Seq[Interface[Component]],
                       override val dependencies: Seq[LinkablePackage],
) extends Package with ContentAddressable{
  override def identifier: Identifier = Identifier(name, PackageEncoder.packageExtensionPrefix)
}

