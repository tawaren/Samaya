package mandalac.structure

import java.io.DataOutputStream

import mandalac.structure.meta.ModuleAttribute
import mandalac.structure.types.Hash

//Todo: Make the interfaces more general so that they can represent Interfaces and concrete code classes
trait Module extends ModuleEssentials {
  def hash: Hash //this is the code hash
  def meta: ModuleMeta

  override protected def serializeMissingDatatype(out:DataOutputStream, index:Int): Unit = {
    //Todo: Rethink / Reanalyze this
    //If we export Interfaces we can fill up missing functions with dummies
    DataType.serializeDefault(out)
  }

  override protected def serializeMissingFunction(out:DataOutputStream, index:Int): Unit = {
    //Todo: Rethink / Reanalyze this
    //If we export Interfaces we can fill up missing functions with dummies
    FunctionDef.serializeDefault(out)
  }

}
