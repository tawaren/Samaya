package samaya.plugin.service

import samaya.types.{Directory, Identifier, OutputTarget}

//A plugin interface to resolve Content Based Addresses
// It just ignores the ones that are not applicable in content addressing mode
trait ReadOnlyAddressResolver extends AddressResolver {
  override def resolveSink(parent: Directory, ident: Identifier.Specific): Option[OutputTarget] = None
  override def deleteDirectory(dir:Directory):Unit = {}
}


