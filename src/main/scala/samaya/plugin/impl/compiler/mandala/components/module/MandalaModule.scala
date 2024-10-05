package samaya.plugin.impl.compiler.mandala.components.module

import samaya.plugin.impl.compiler.mandala.components.MandalaComponent
import samaya.plugin.impl.compiler.mandala.entry.TypeAlias
import samaya.structure.types.CompLink
import samaya.structure.{Interface, Meta, Module}

trait MandalaModule extends Module with MandalaComponent {
  def instances: Map[CompLink, Seq[String]]
  def typeAlias: Seq[TypeAlias]
  override def toInterface(meta: Meta): Interface[MandalaModule]
}

object MandalaModule {
  def deriveOverloadedName(name:String, args:Int):String = name +"$"+args
}