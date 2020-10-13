package samaya.plugin.impl.compiler.mandala.compiler

import samaya.plugin.impl.compiler.mandala.components.instance.Instance
import samaya.plugin.impl.compiler.mandala.components.instance.Instance.{LocalEntryRef, RemoteEntryRef}
import samaya.plugin.impl.compiler.mandala.components.module.MandalaModuleCompilerOutput
import samaya.plugin.impl.compiler.simple.MandalaParser
import samaya.structure.types.Accessibility.Local
import samaya.structure.types.Type
import samaya.structure.Module

import scala.collection.JavaConverters._

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

    val (instances, implMod) = withComponentBuilder(name, systemMode) {
      withDefaultAccess(Local) {
        ctx.moduleEntry().asScala.map(visitModuleEntry)
      }
      assert(currentComponent._signatures.isEmpty)
      val mainModule = build(new MandalaModuleCompilerOutput(
        name,
        systemMode,
        currentComponent._dataTypes,
        currentComponent._functions,
        currentComponent._implements,
        currentComponent._instances
      ),sourceIdFromContext(ctx))
      (currentComponent.localInstances, mainModule)
    }

    //now we build the instanceDefs
    implMod match {
      case Some(impl) => for(inst <- instances) {
        val implDefName = Instance.deriveTopName(name, inst.name)
        withComponentBuilder(implDefName){
          //Note: These were generated in the context of the main Module (implMod) but are now referenzed from the instance
          //      This means we must globalize the types manually here, because we do not access them over a Func, which usually takes care of this
          val globalApplies = inst.applies.map(Type.globalizeLocals(impl.link,_))
          val globalizedFunRefs = inst.funReferences.map{
            case (name, remote:RemoteEntryRef) => (name, remote)
            case (name, LocalEntryRef(offset)) => (name, RemoteEntryRef(impl.link, offset))
          }
          val globalizedImplRefs = inst.implReferences.map{
            case (name, remote:RemoteEntryRef) => (name, remote)
            case (name, LocalEntryRef(offset)) => (name, RemoteEntryRef(impl.link, offset))
          }
          val res = defInstance(implDefName,inst.classTarget,globalApplies,globalizedFunRefs, globalizedImplRefs);
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
