package samaya.plugin.service.category

import samaya.plugin.service.{DebugAssembler, PluginCategory}

object DebugAssemblerPluginCategory extends PluginCategory[DebugAssembler]{
  override def name: String = "debug_assembler"
  override def interface: Class[DebugAssembler] = classOf[DebugAssembler]
}
