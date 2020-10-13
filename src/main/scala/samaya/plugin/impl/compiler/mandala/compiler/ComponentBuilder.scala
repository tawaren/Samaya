package samaya.plugin.impl.compiler.mandala.compiler

import samaya.compilation.ErrorManager._
import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.plugin.impl.compiler.mandala.components.instance.Instance
import samaya.plugin.impl.compiler.mandala.components.module.{MandalaModule, MandalaModuleInterface}
import samaya.plugin.impl.compiler.mandala.entry.instance.LocalInstanceEntry
import samaya.structure.types.CompLink
import samaya.structure.{Attribute, DataDef, FunctionDef, FunctionSig, ImplementDef, Interface, Meta, Module, ModuleEntry, ModuleInterface, Package, SignatureDef}
import samaya.types.Context


//Helper to not pollute the main visitor
trait ComponentBuilder extends CompilerToolbox{

  class ComponentBuilder(override val name:String, override val mode: Module.Mode) extends MandalaModule {
    override val language: String = MandalaCompiler.Language
    override val version: String = MandalaCompiler.Version
    override val classifier: Set[String] = Set("mandala", "placeholder")
    override val attributes: Seq[Attribute] = Seq.empty
    override val isVirtual: Boolean = true //this should never end up in compilation but to be sure set it virtual

    override def dataTypes:Seq[DataDef] = _dataTypes
    override def signatures:Seq[FunctionSig] = _signatures
    override def functions:Seq[FunctionSig] = _functions
    override def implements:Seq[FunctionSig] = _implements
    override def instances:Map[CompLink, Seq[String]] = _instances

    override def toInterface(meta: Meta): Interface[MandalaModule] = new MandalaModuleInterface(meta, this)

    var availableEntries:Map[String,ModuleEntry] = Map.empty
    var instanceNames:Set[String] = Set.empty
    var localInstances: Seq[LocalInstanceEntry] = Seq.empty

    var _dataTypes: Seq[DataDef] = Seq.empty
    var _signatures: Seq[SignatureDef] = Seq.empty
    var _functions: Seq[FunctionDef] = Seq.empty
    var _implements: Seq[ImplementDef] = Seq.empty
    var _instances: Map[CompLink, Seq[String]] = Map.empty

  }

  var currentComponent:ComponentBuilder = null

  def withComponentBuilder[T](name:String, mode: Module.Mode = Module.Normal)(body: => T):T = {
    val oldBuilder = currentComponent
    currentComponent = new ComponentBuilder(name, mode)
    val res = body
    currentComponent = oldBuilder
    res
  }

  def localEntries:Map[String,ModuleEntry] = currentComponent.availableEntries
  def context: Context = Context(Option(currentComponent), env.pkg)

  private def register[T <: ModuleEntry](comp:T)(update:(T) => Unit):T = {
    if(currentComponent.availableEntries.contains(comp.name) || currentComponent.instanceNames.contains(comp.name)){
      feedback(PlainMessage(s"Entry with name ${comp.name} already exists", Error))
    }

    currentComponent.availableEntries = currentComponent.availableEntries.updated(comp.name,comp)
    update(comp)
    comp
  }

  def registerInstanceEntry(entr:LocalInstanceEntry):LocalInstanceEntry = {
    if(currentComponent.availableEntries.contains(entr.name) || currentComponent.instanceNames.contains(entr.name)){
      feedback(PlainMessage(s"Entry with name ${entr.name} already exists", Error))
    }
    currentComponent.instanceNames = currentComponent.instanceNames + entr.name
    currentComponent.localInstances = currentComponent.localInstances :+ entr
    val trg = entr.classTarget
    val instName = Instance.deriveTopName(currentComponent.name,entr.name)
    currentComponent._instances = currentComponent._instances.updated(trg, instName +: currentComponent._instances.getOrElse(trg, Seq.empty))
    env.instRec(entr, isLocal = true)
    entr
  }

  def registerDataDef(comp:DataDef):DataDef = register(comp){c => currentComponent._dataTypes = currentComponent._dataTypes :+ c}
  def registerSignatureDef(comp:SignatureDef):SignatureDef = register(comp){c => currentComponent._signatures = currentComponent._signatures :+ c}
  def registerImplementDef(comp:ImplementDef):ImplementDef = register(comp){c => currentComponent._implements = currentComponent._implements :+ c}
  def registerFunctionDef(comp:FunctionDef):FunctionDef = register(comp){ c => currentComponent._functions = currentComponent._functions :+ c}


  def nextDataIndex():Int = currentComponent._dataTypes.size
  def nextSigIndex():Int = currentComponent._signatures.size
  def nextImplIndex():Int = currentComponent._implements.size
  def nextFunIndex():Int = currentComponent._functions.size
  def nextPosition():Int = currentComponent.availableEntries.size

}