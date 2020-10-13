package samaya.plugin.impl.compiler.mandala.entry.instance

import samaya.plugin.impl.compiler.mandala.components.InstInfo
import samaya.plugin.impl.compiler.mandala.components.instance.Instance.EntryRef
import samaya.structure.types.{CompLink, Func, ImplFunc, SourceId, Type}

case class LocalInstanceEntry(
   name:String,
   override val classTarget:CompLink,
   override val funReferences: Map[String, EntryRef],
   override val implReferences: Map[String, EntryRef],
   override val applies:Seq[Type],
   src:SourceId,
) extends InstInfo
