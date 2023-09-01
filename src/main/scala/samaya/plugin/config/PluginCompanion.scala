package samaya.plugin.config

import samaya.config.Configurable

trait PluginCompanion extends Configurable{
  //Allows to overwrite plugin instantiation
  def init[T](pluginClass:Class[T]):T = pluginClass.getDeclaredConstructor().newInstance()
}
