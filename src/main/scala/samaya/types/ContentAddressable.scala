package samaya.types

import samaya.structure.types.Hash

trait ContentAddressable extends Addressable {
  def hash: Hash
}
