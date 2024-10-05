package samaya.plugin.impl.compiler.mandala.components.instance

import samaya.plugin.impl.compiler.mandala.components.MandalaComponent
import samaya.structure.types.{CompLink, Type}
import samaya.structure.{Component, Generic, Interface, Meta, TypeParameterized}

trait Instance extends MandalaComponent with TypeParameterized {
  def classTarget:CompLink
  def classApplies:Seq[Type]
  def toInterface(meta: Meta): Interface[Instance]
  def generic(index:Int):Option[Generic] = generics.find(gi => gi.index == index)
}

object Instance {
  def deriveTopName(moduleName: String, implName: String):String = moduleName+"$"+implName
}