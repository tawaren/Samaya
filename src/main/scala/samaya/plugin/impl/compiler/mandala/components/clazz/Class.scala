package samaya.plugin.impl.compiler.mandala.components.clazz

import samaya.plugin.impl.compiler.mandala.components.MandalaComponent
import samaya.structure.{Attribute, FunctionSig, Generic, Interface, Meta, Module, TypeParameterized}

trait Class extends Module with MandalaComponent with TypeParameterized {
  override def attributes: Seq[Attribute] = Seq.empty
  override def implements: Seq[FunctionSig] = Seq.empty
  def generic(index:Int):Option[Generic] = generics.find(gi => gi.index == index)
  def toInterface(meta: Meta): Interface[Class]
}
