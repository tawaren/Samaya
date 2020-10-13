package samaya.plugin.impl.compiler.mandala.components.module

import samaya.structure.types.CompLink
import samaya.structure.{Interface, Meta, Module}

trait MandalaModule extends Module {
  def instances: Map[CompLink, Seq[String]]
  override def toInterface(meta: Meta): Interface[MandalaModule]
}
