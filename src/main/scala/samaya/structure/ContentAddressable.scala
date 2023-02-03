package samaya.structure

import samaya.structure.types.Hash
import samaya.types.{Identifier, Directory}

trait ContentAddressable {
  def hash:Hash
  def location:Directory
  def identifier:Identifier
}
