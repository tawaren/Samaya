package samaya.plugin.impl.inter.json

import samaya.structure.types.{Accessibility, Capability, Permission}
import samaya.structure
import samaya.structure.{Attribute, Generic, Param, Result}

case class FunctionSigImpl(override val location: JsonLocation, input:JsonModel.FunctionSignature) extends structure.FunctionSig with JsonSource {
  override val index: Int = input.offset
  override val name: String = input.name
  override val position: Int = input.position
  override def attributes: Seq[Attribute] = input.attributes
  override val transactional: Boolean = input.transactional
  override val capabilities: Set[Capability] = input.capabilities.flatMap(c => Capability.fromString(c))
  override val accessibility: Map[Permission, Accessibility] = input.accessibility.flatMap {
    case (perm, access) =>
      for(p <- Permission.fromString(perm); a <- Accessibility.fromString(access.name,access.guards))
        yield (p,a)
  }

  override val generics: Seq[Generic] = {
    val genLoc = location.descendProperty("generics")
    input.generics.zipWithIndex.map(gi => GenericImpl(genLoc.descendProperty(gi._1.name), gi._1,gi._2))
  }

  override val params: Seq[Param] = TypeBuilder.inContext(generics){
    val paramLoc = location.descendProperty("params")
    input.params.zipWithIndex.map(pi => ParamImpl(paramLoc.descendProperty(pi._1.name),pi._1,pi._2))
  }

  override val results: Seq[Result] = TypeBuilder.inContext(generics){
    val resultLoc = location.descendProperty("results")
    input.returns.zipWithIndex.map(ri => ReturnImpl(resultLoc.descendProperty(ri._1.name), ri._1,ri._2))
  }

}
