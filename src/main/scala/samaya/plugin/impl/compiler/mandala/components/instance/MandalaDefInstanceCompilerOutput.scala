package samaya.plugin.impl.compiler.mandala.components.instance

import samaya.plugin.impl.compiler.mandala.components.instance.Instance.EntryRef
import samaya.structure.{Interface, Meta}
import samaya.structure.types.{CompLink, Type}

class MandalaDefInstanceCompilerOutput(
  override val name:String,
  override val classTarget: CompLink,
  override val applies: Seq[Type],
  override val funReferences: Map[String, EntryRef],
  override val implReferences: Map[String, EntryRef]
) extends DefInstance {
  override def toInterface(meta: Meta): Interface[DefInstance] = new DefInstanceInterface(meta, this)
}