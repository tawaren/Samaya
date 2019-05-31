package mandalac.registries

import mandalac.structure.Module
import mandalac.structure.types.Hash
import mandalac.types.Identifier

import scala.collection.mutable
import scala.util.DynamicVariable

object ModuleRegistry{


  private val currentModule:DynamicVariable[Option[Module]] = new DynamicVariable(None)


  private val byName = new mutable.HashMap[(Seq[Identifier],String,String),Module]
  def moduleByName(pkg: Seq[Identifier], name: String, classifier:String): Option[Module] = {
    if(currentModule.value.map(m => m.name).orNull != name ){
      byName.get((pkg, name, classifier))
    } else {
      currentModule.value
    }
  }

  private val byHash = new mutable.HashMap[Hash,Module]
  def moduleByHash(id: Hash): Option[Module] = {
    if(currentModule.value.map(m => m.hash).orNull != id ){
      byHash.get(id)
    } else {
      currentModule.value
    }
  }


  def recordModule(pkg: Seq[Identifier], mod:Module):Unit = {
    byHash.put(mod.hash,mod )
    byName.put((pkg,mod.name,mod.classifier),mod)
  }

  def executeInModule[T](mod:Module)(b: => T):T = currentModule.withValue(Some(mod))(b)


}
