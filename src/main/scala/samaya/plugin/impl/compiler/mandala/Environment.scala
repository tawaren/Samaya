package samaya.plugin.impl.compiler.mandala

import samaya.plugin.impl.compiler.mandala.components.InstInfo
import samaya.plugin.impl.compiler.mandala.components.instance.DefInstance
import samaya.plugin.impl.compiler.mandala.entry.instance.LocalInstanceEntry
import samaya.structure.types.CompLink
import samaya.structure.{Component, Interface, Package}

abstract class Environment(val file:String, private var curPkg: Package) {
  def innerBuild(pkg:Package,comp:Component):(Package,Option[Interface[Component]])
  var globalInstances = Map.empty[CompLink,Seq[InstInfo]]
  var localInstances = Map.empty[CompLink,Seq[InstInfo]]

  def pkg:Package = curPkg

  def instRec(inst:InstInfo, isLocal:Boolean):Unit = {
    if(isLocal) {
      localInstances = localInstances.updated(inst.classTarget, inst +: localInstances.getOrElse(inst.classTarget,Seq.empty))
    } else {
      globalInstances = globalInstances.updated(inst.classTarget, inst +: globalInstances.getOrElse(inst.classTarget, Seq.empty))
    }
  }

  def builder(comp:Component):Option[Interface[Component]] = {
    val (newPkg, builtComp) = innerBuild(curPkg,comp)
    localInstances = Map.empty[CompLink,Seq[InstInfo]]
    curPkg = newPkg
    builtComp
  }
}
