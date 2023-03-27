package samaya.types

import samaya.structure.types.Hash

trait RepositoryBuilder {
  def indexContent(cont:ContentAddressable):Unit
  def result():Map[Hash, ContentAddressable]
}
