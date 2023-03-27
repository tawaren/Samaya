package samaya.types

trait Addressable {
  def location: Directory
  def identifier: Identifier
}
