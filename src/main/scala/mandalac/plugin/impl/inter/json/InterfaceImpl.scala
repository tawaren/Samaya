package mandalac.plugin.impl.inter.json

import mandalac.structure.meta.ModuleAttribute
import mandalac.structure.types.Hash
import mandalac.structure
import mandalac.structure.{DataType, Module, ModuleMeta, Risk}

class InterfaceImpl(input:JsonModel.Interface, override val hash: Hash, override val meta:ModuleMeta) extends Module{
  override val name: String = input.name
  override val language: String = input.language
  override val version: String = input.version
  override val classifier: String = input.classifier

  override val attributes: Seq[ModuleAttribute] = ???

  override val functions: Seq[structure.FunctionDef] = input.functions.map(inp => FunctionImpl(inp))
  override def function(index: Int): Option[structure.FunctionDef] = functions.find(f => f.index==index)
  override val dataTypes: Seq[DataType] = input.datatypes.map(inp => DataTypeImpl(inp))
  override def dataType(index: Int):Option[DataType] = dataTypes.find(f => f.index==index)
  override val risks: Seq[Risk] = input.risks.map(inp => RiskImpl(inp))
  override def risk(index: Int): Option[Risk] = risks.find(f => f.index==index)
}
