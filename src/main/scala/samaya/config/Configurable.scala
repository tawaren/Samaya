package samaya.config

trait Configurable {
  def configure(main:ParameterAndOptions, base:ParameterAndOptions, plugin:ParameterAndOptions = ParameterAndOptions.empty):Unit
  def help():Unit = {}
  def status():Unit = {}
}
