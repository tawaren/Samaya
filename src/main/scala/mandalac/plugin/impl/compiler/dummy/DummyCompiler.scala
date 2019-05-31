package mandalac.plugin.impl.compiler.dummy

import mandalac.plugin.service.Selectors.{CompilerSelectorByMeta, CompilerSelectorBySource}
import mandalac.plugin.service.{LanguageCompiler, Selectors}
import mandalac.{structure, types}
import mandalac.types.InputSource

object DummyCompiler {
  val Language:String = "dummy"
  val Version:String = "0"
  val classifiers = Seq("dummy")
}

class DummyCompiler extends LanguageCompiler{
  override def matches(s: Selectors.CompilerSelector): Boolean = s match {
    case CompilerSelectorByMeta(DummyCompiler.Language, DummyCompiler.Version, classifier) => DummyCompiler.classifiers.contains(classifier)
    case CompilerSelectorBySource(source)  => source.identifier.extension.contains(DummyCompiler.Language)
    case _ => false
  }


  override def compileSpecific(language: String, version: String, classifier: String, source: InputSource, env: types.Package): Option[structure.Module] = {
    Some(new DummyCompilerOutput(source.identifier.name))
  }

  override def extractDependencies(source: InputSource): Set[Seq[String]] = Set.empty
  override def compileFully(source: InputSource, pkg: types.Package): Seq[structure.Module] = {
    Seq(new DummyCompilerOutput(source.identifier.name))
  }

}
