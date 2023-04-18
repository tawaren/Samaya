package samaya.structure

import samaya.plugin.service.PackageEncoder
import samaya.structure.types.Hash
import samaya.types.{ContentAddressable, Directory, Identifier}

class LinkablePackage(
                       val interfacesOnly:Boolean,
                       val location: Directory,
                       val hash:Hash,
                       override val name: String,
                       //Note: The order must comply with deployment order
                       //       meaning dependencies must come before use
                       override val components: Seq[Interface[Component]],
                       override val dependencies: Seq[LinkablePackage],
                       val includes:Option[Set[String]],
                     ) extends Package with ContentAddressable{
  override def identifier: Identifier = Identifier(name, PackageEncoder.packageExtensionPrefix+Identifier.wildcard)
}

