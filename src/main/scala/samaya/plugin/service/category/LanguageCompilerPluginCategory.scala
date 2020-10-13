package samaya.plugin.service.category

import samaya.plugin.service.{LanguageCompiler, PluginCategory}

object LanguageCompilerPluginCategory extends PluginCategory[LanguageCompiler]{
  override def name: String = "language_compiler"
  override def interface: Class[LanguageCompiler] = classOf[LanguageCompiler]
}
