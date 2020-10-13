package samaya.plugin.impl.inter.json

import samaya.structure.{Attribute, Param, Result, Transaction}

class TransactionInterfaceImpl(override val location: JsonLocation, input:JsonModel.InterfaceTransaction) extends Transaction with JsonSource {
  override val name: String = input.name
  override val language: String = input.language
  override val version: String = input.version
  override val classifier: Set[String] = input.classifier
  override val attributes: Seq[Attribute] = input.attributes
  override val isVirtual: Boolean = false

  override def transactional: Boolean = input.transactional

  override val params: Seq[Param] = TypeBuilder.inContext(Seq.empty){
    val paramLoc = location.descendProperty("params")
    input.params.zipWithIndex.map(pi => ParamImpl(paramLoc.descendProperty(pi._1.name),pi._1,pi._2))
  }

  override val results: Seq[Result] = TypeBuilder.inContext(Seq.empty){
    val resultLoc = location.descendProperty("results")
    input.returns.zipWithIndex.map(ri => ReturnImpl(resultLoc.descendProperty(ri._1.name), ri._1,ri._2))
  }
}
