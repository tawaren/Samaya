package mandalac.plugin.service.category

import mandalac.plugin.service.{LanguageCompiler, PluginCategory}

object LanguageCompilerPluginCategory extends PluginCategory[LanguageCompiler]{
  override def name: String = "language_compiler"
  override def interface: Class[LanguageCompiler] = classOf[LanguageCompiler]
}
