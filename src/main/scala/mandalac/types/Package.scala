package mandalac.types

import mandalac.structure.Module
import mandalac.structure.types.Hash

trait Package {
  def path:Seq[Identifier]
  def location:Location
  def name:String
  def hash:Hash
  def modules:Seq[Module]
  def moduleByName(name: String, classifier:String): Option[Module] = modules.find(e => e.name == name && e.classifier == classifier)
  def allModulesByName(name: String): Seq[Module] = modules.filter(e => e.name == name)
  def moduleByHash(hash: Hash): Option[Module] = modules.find(e => e.hash == hash)

  //Helper to recurse down
  def allModulesByPath(path:Seq[String]):Seq[Module] ={
    if(path.size == 1) {
      allModulesByName(path.head)
    } else if(path.size > 1) {
      dependencyByName(path.head).map(other => other.allModulesByPath(path.tail)).getOrElse(Seq.empty)
    } else {
      Seq.empty
    }
  }

  def moduleByPath(path:Seq[String], classifier:String):Option[Module] ={
    allModulesByPath(path).find(p => p.classifier == classifier)
  }

  //todo: have a reexport Flag and ev a rename (No Rename and all reexport behaves as now)
  def dependencies:Seq[Package]
  def dependencyByName(name: String): Option[Package] = dependencies.find(p => p.name == name)
  def dependencyByHash(hash: Hash): Option[Package] = dependencies.find(p => p.hash == hash)



}
