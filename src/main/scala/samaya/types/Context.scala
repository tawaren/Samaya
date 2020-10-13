package samaya.types

import samaya.structure.{Module, Package}

trait Context {
  def module:Option[Module]
  def pkg:Package
}

object Context {
  def apply(mod:Option[Module], pakage:Package):Context = new Context {
    override def module: Option[Module] = mod
    override def pkg: Package = pakage
  }

  def apply(pakage:Package):Context = apply(None, pakage)
  def apply(mod:Module, pakage:Package):Context = apply(Some(mod), pakage)
}

