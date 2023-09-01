package samaya.config

object Config extends ConfigCompanion {
  def init(args:Array[String]):Unit = {
    ConfigHolder.init(args)
    configure(ConfigHolder.main,ConfigHolder.base)
  }

  val command = param(0)
  val verbose = opt("v|verbose")

  //features
  val forwardRef = opt("forward_ref").default(true)
  val dataIndexes = opt("data_indexes").default(false)
}
