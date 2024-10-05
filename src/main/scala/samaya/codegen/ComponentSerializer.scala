package samaya.codegen

import java.io.DataOutputStream
import samaya.compilation.ErrorManager._
import samaya.plugin.service.ComponentTypeResolver
import samaya.structure.types.Hash
import samaya.structure.{CompiledModule, CompiledTransaction, Component, Package}
import samaya.types.Context

object ComponentSerializer {


  def serialize(out: DataOutputStream, cmp: Component,sourceHash:Hash, pkg: Package): Unit = {
    cmp match {
      case module: CompiledModule => ModuleSerializer.serialize(out, module, sourceHash, pkg)
      case txt: CompiledTransaction =>
        val context = Context(pkg)
        ModuleSerializer.serializeFunction(out,txt,context, true)
      case other => unexpected(s"Component $other can not be used in codegen", CodeGen())
    }
  }

  private val defaultExtension = "sans"

  def generateExtensionName(classifiers: Set[String]):String = {
    ComponentTypeResolver.resolveType(classifiers).map(_.extension).getOrElse(defaultExtension)
  }

  def generateExtensionName(component: Component):String = {
    ComponentTypeResolver.resolveType(component).map(_.extension).getOrElse(defaultExtension)
  }


}
