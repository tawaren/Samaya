package mandalac.plugin.service

import mandalac.plugin.service.category.LanguageCompilerPluginCategory
import mandalac.plugin.{Plugin, PluginProxy}
import mandalac.structure.{Module, ModuleEssentials}
import mandalac.types.{InputSource, Package}

//a plugin description for a compiler that can compile sources in a language to sanskrit code
trait LanguageCompiler extends Plugin {

  //A Compilation Task description intended for selecting the appropriate plugin
  override type Selector = Selectors.CompilerSelector

  //compiles source by using env to resolve dependencies
  def compileSpecific(language:String, version:String, classifier:String, source: InputSource, env:Package):Option[ModuleEssentials]
  def extractDependencies(source: InputSource):Set[Seq[String]]
  def compileFully(source: InputSource, pkg:Package):Seq[ModuleEssentials]
}

object LanguageCompiler extends LanguageCompiler with PluginProxy{

  type PluginType = LanguageCompiler
  override def category: PluginCategory[PluginType] = LanguageCompilerPluginCategory


  //compiles source by using env to resolve dependencies
  def compileSpecific(language:String, version:String, classifier:String, source: InputSource, env:Package):Option[ModuleEssentials] = {
    select(Selectors.CompilerSelectorByMeta(language, version, classifier)).flatMap(r => r.compileSpecific(language, version, classifier, source, env))
  }

  override def compileFully(source: InputSource, pkg:Package): Seq[ModuleEssentials] = {
    selectAll(Selectors.CompilerSelectorBySource(source)).flatMap(c => c.compileFully(source,pkg))
  }

  override def extractDependencies(source: InputSource): Set[Seq[String]] = {
    selectAll(Selectors.CompilerSelectorBySource(source)).flatMap(c => c.extractDependencies(source)).toSet
  }

}
