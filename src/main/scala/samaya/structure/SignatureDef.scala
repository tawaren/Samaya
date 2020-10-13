package samaya.structure

import samaya.structure.types.{Capability, SourceId}

trait SignatureDef extends FunctionSig with ModuleEntry {
  def capabilities:Set[Capability]
}
