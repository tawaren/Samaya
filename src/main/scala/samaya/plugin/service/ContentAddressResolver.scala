package samaya.plugin.service

import samaya.plugin.Plugin
import samaya.plugin.service.AddressResolver.DirectoryMode
import samaya.types.{Address, ContentAddressable, Directory, Identifier, OutputTarget}

//A plugin interface to resolve Content Based Addresses
// It just ignores the ones that are not applicable in content addressing mode
trait ContentAddressResolver extends ReadOnlyAddressResolver {
  //Todo: in theory we could content address directories
  override def resolveDirectory(path: Address, mode:DirectoryMode): Option[Directory] = None
  override def list(parent: Directory, filter: Option[AddressResolver.AddressKind]): Set[Identifier] = Set.empty
  override def serializeDirectoryAddress(target: Directory, mode: AddressResolver.SerializerMode): Option[String] = None
  override def provideDefault(): Option[Directory] = None
}


