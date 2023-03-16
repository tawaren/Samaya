package samaya.plugin.service

import samaya.build.desc.Dependency
import samaya.plugin.service.AddressResolver.PluginType
import samaya.plugin.service.Selectors.CompilerSelectorByIdentifier
import samaya.plugin.service.category.LanguageCompilerPluginCategory
import samaya.plugin.{Plugin, PluginProxy}
import samaya.structure
import samaya.structure.{CompiledModule, Component, Interface, Module, ModuleInterface, Transaction}
import samaya.types.{Identifier, InputSource}

import scala.reflect.ClassTag

//a plugin description for a compiler that can compile sources in a language to sanskrit code
trait LanguageCompiler extends Plugin {

  //A Compilation Task description intended for selecting the appropriate plugin
  override type Selector = Selectors.CompilerSelector

  //compiles source by using env to resolve dependencies
  def compileAndBuildFully(source: InputSource, pkg:structure.Package)(builder: Component => (structure.Package, Option[Interface[Component]])):structure.Package

  def extractDependencies(source: InputSource):Set[Dependency]
  def extractComponentNames(source: InputSource):Set[String]
}

object LanguageCompiler extends LanguageCompiler with PluginProxy{

  type PluginType = LanguageCompiler
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = LanguageCompilerPluginCategory

  def canCompile(identifier:Identifier):Boolean = {
    select(CompilerSelectorByIdentifier(identifier), silent = true).isDefined
  }

  override def compileAndBuildFully(source: InputSource, pkg:structure.Package)(builder: Component => (structure.Package, Option[Interface[Component]])):structure.Package = {
    selectAll(Selectors.CompilerSelectorBySource(source)).foldLeft(pkg) {
      case (pkg, comp) => comp.compileAndBuildFully(source, pkg)(builder)
    }
  }

  override def extractDependencies(source: InputSource): Set[Dependency] = {
    selectAll(Selectors.CompilerSelectorBySource(source)).flatMap(c => c.extractDependencies(source)).toSet
  }

  override def extractComponentNames(source: InputSource): Set[String] = {
    selectAll(Selectors.CompilerSelectorBySource(source)).flatMap(r => r.extractComponentNames(source)).toSet
  }
}
