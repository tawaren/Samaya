package samaya.plugin.impl.compiler.mandala.compiler

import samaya.plugin.impl.compiler.mandala.MandalaParser
import samaya.plugin.impl.compiler.mandala.components.instance.Instance
import samaya.plugin.impl.compiler.mandala.components.module.MandalaModuleCompilerOutput
import samaya.plugin.impl.compiler.mandala.entry.SigImplement
import samaya.structure.types.Accessibility.Local
import samaya.structure.types.{Func, ImplFunc, StdFunc, Type}
import samaya.structure.Module

import scala.jdk.CollectionConverters._

trait ModuleBuilder extends CompilerToolbox{
  self: ComponentBuilder with InstanceBuilder with PermissionCompiler =>

  override def visitModule(ctx: MandalaParser.ModuleContext): Unit = {
    val name = visitName(ctx.name)
    val systemMode = if(ctx.system() != null) {
      if(ctx.system().id != null) {
        Module.Precompile(ctx.system().id.getText.toInt)
      } else {
        Module.Elevated
      }
    } else {
      Module.Normal
    }

    val (instances, implInstMod) = withComponentBuilder(name, systemMode) {
      withDefaultAccess(Local) {
        ctx.moduleEntry().asScala.map(visitModuleEntry)
      }
      assert(currentComponent._signatures.isEmpty)
      val src = sourceIdFromContext(ctx)
      val mainModule = build(new MandalaModuleCompilerOutput(
        name,
        systemMode,
        currentComponent._dataTypes,
        currentComponent._functions,
        currentComponent._implements,
        currentComponent._instances,
        currentComponent._typeAlias,
        src
      ),src)
      (currentComponent.localInstances, mainModule)
    }

    //now we build the instanceDefs
    implInstMod match {
      case Some(implInst) => for(inst <- instances) {
        val implDefName = Instance.deriveTopName(name, inst.name)
        withComponentBuilder(implDefName){
          //Note: These were generated in the context of the main Module (implMod) but are now referenzed from the instance
          //      This means we must globalize the types manually here, because we do not access them over a Func, which usually takes care of this
          val globalizedClassApplies = inst.classApplies.map(Type.globalizeLocals(implInst.link,_))
          val globalizedImplements = inst.implements.map{
            case SigImplement(name,gens,fun,impl, src) =>
              val gFun = Func.globalizeLocals(implInst.link,fun).asInstanceOf[StdFunc]
              val gImpl = Func.globalizeLocals(implInst.link,impl).asInstanceOf[ImplFunc]
              SigImplement(name,gens, gFun, gImpl,src)
          }
          val res = defInstance(implDefName,inst.generics, inst.classTarget,globalizedClassApplies,globalizedImplements,inst.src)
          build(res,inst.src)
        }
      }
      case None =>
        //ups main failed, so no instances possible
    }


  }

  override def visitModuleEntry(ctx: MandalaParser.ModuleEntryContext): Unit = {
    withFreshCounters { super.visitModuleEntry(ctx) }
  }

}
