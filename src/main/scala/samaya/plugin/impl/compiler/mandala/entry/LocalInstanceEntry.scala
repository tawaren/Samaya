package samaya.plugin.impl.compiler.mandala.entry

import samaya.plugin.impl.compiler.mandala.components.InstInfo
import samaya.structure.Generic
import samaya.structure.types.{CompLink, ImplFunc, SourceId, StdFunc, Type}

case class LocalInstanceEntry(
     name:String,
     override val generics:Seq[Generic],
     override val classTarget:CompLink,
     override val implements: Seq[SigImplement],
     override val classApplies:Seq[Type],
     src:SourceId,
) extends InstInfo
