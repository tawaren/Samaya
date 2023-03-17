package samaya.build

import samaya.structure.{Component, Interface, LinkablePackage, Package}
import samaya.types.Directory

trait PartialPackage[P <: PartialPackage[P]] extends Package{
  this : P =>
  def withComponent(comp:Interface[Component]):P
  def toLinkablePackage(placement:Directory): LinkablePackage
}
