package samaya.config

//todo have a better one
//  1. is class takes a name alla (json.package.encoding)
//  2. it has opt("interface-format|format")
//     only one is found with prio pluginArgs < baseArgs < mainArgs
//     if one has both interface-format & format then the former takse precendece
//    Special: it looks for: format, encoder.format, package.encoding.format & json.package.encoding.format
//             the last one has precendence
//  3. multiOpt same as opt but expects more then one entry
//  4. collectOpt same as multiOpt but collestc all instead of applying precedence
//  5. param(0) - gets the first param from: mainArgs.params ++ baseArgs.params ++  pluginArgs.params
//  6. params - gets mainArgs.params ++ baseArgs.params ++  pluginArgs.params
//  They return a ConfigValue with a get() <-- mut be called late
//   internally uses lazy val
//  Advantage - we can configure & use in help() / status()
//   example: opt("interface-format|format").description("selects the interface serialisation plugin")
//            opt("p|port").asInt
//  Further: opt("interface-format|format").main -- uses the main argument list explicitly
trait RawConfigCompanion extends Configurable {
  private var mainArgs : ParameterAndOptions = null
  private var pluginArgs : ParameterAndOptions = null
  private var baseArgs : ParameterAndOptions = null

  override def configure(_main: ParameterAndOptions, _base: ParameterAndOptions, _plugin: ParameterAndOptions): Unit = {
    mainArgs = _main
    pluginArgs = _plugin
    baseArgs = _base
  }

  def main : ParameterAndOptions = mainArgs
  def plugin : ParameterAndOptions = pluginArgs
  def base : ParameterAndOptions = baseArgs

}
