package samaya.plugin.impl.compiler.mandala

import samaya.plugin.impl.compiler.mandala.components.InstInfo
import samaya.plugin.impl.compiler.mandala.components.instance.Instance
import samaya.structure.types.CompLink
import samaya.structure.{CompiledTransaction, Component, DataDef, FunctionDef, ImplementDef, Interface, Module, ModuleEntry, Package, SignatureDef, Transaction}
import samaya.toolbox.transform.EntryTransformer
import samaya.types.{ContentAddressable, Context}

abstract class Environment(val source:ContentAddressable, private var curPkg: Package) {
  def buildComponent(comp:Component):(Package,Option[Interface[Component]])
  def createPipeline():EntryTransformer

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
    val transformed = comp match {
      case transaction: CompiledTransaction => createPipeline().transformTransaction(transaction, pkg)
      case cmp => cmp
    }

    val (newPkg, builtComp) = buildComponent(transformed)
    localInstances = Map.empty[CompLink,Seq[InstInfo]]
    curPkg = newPkg
    builtComp
  }

  def builder(mod:Module, entry: FunctionDef): FunctionDef = {
    createPipeline().transformFunction(entry, Context(mod,curPkg))
  }

  def builder(mod:Module, entry: ImplementDef): ImplementDef = {
    createPipeline().transformImplement(entry, Context(mod,curPkg))
  }

  def builder(mod:Module, entry: SignatureDef): SignatureDef = {
    createPipeline().transformSignature(entry, Context(mod,curPkg))
  }

  def builder(mod:Module, entry: DataDef): DataDef = {
    createPipeline().transformDataType(entry, Context(mod,curPkg))
  }

}
