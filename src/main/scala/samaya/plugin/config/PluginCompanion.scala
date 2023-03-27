package samaya.plugin.config

trait PluginCompanion {
  def configure(main:ParameterAndOptions, base:ParameterAndOptions, plugin:ParameterAndOptions):Unit
  def help():Unit = {}
  def status():Unit = {}
  //Allows to overwrite plugin instantiation
  def init[T](pluginClass:Class[T]):T = pluginClass.getDeclaredConstructor().newInstance()
}
