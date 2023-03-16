package samaya.plugin.service

import samaya.plugin.Plugin
import samaya.structure.ContentAddressable
import samaya.types.{Address, Directory, Identifier, OutputTarget}

//A plugin interface to resolve Content Based Addresses
// It just ignores the ones that are not applicable in content addressing mode
trait ContentAddressResolver extends AddressResolver {
  override def resolveDirectory(parent: Directory, path: Address, create: Boolean): Option[Directory] = None
  override def resolveSink(parent: Directory, ident: Identifier.Specific): Option[OutputTarget] = None
  override def listSources(parent: Directory): Set[Identifier] = Set.empty
  override def listDirectories(parent: Directory): Set[Identifier] = Set.empty
  override def serializeDirectory(parent: Option[Directory], target: Directory): Option[String] = None
  override def provideDefault(): Option[Directory] = None
  override def deleteDirectory(dir:Directory):Unit = {}
}


