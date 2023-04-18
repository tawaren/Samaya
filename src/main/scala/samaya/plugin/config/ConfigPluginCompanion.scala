package samaya.plugin.config

import samaya.plugin.config.ConfigValue.ConfigCell

trait ConfigPluginCompanion extends PluginCompanion {
  private var values : Seq[ConfigCell[_]] = Seq.empty

  private def addAndReturn[T](v:ConfigCell[T]):ConfigValue[T] ={
    values = values :+ v
    v
  }


  private def collectKey(key:String)(main: ParameterAndOptions, base: ParameterAndOptions, plugin: ParameterAndOptions):Option[Seq[String]] = {
    val sources = LazyList(main, base, plugin)
    val fields = LazyList.from(key.split("\\|").map(_.trim))
    val res = sources.flatMap(o => fields.map(o.options.get).find(_.isDefined).flatten.getOrElse(Seq.empty))
    if(res.isEmpty){
      None
    } else {
      Some(res)
    }
  }

  private def getKey(key:String)(main: ParameterAndOptions, base: ParameterAndOptions, plugin: ParameterAndOptions):Option[String] = {
    val sources = LazyList(main, base, plugin)
    val fields = LazyList.from(key.split("\\|").map(_.trim))
    val res = sources
      .map(o => fields.map(o.options.get).find(p => p.isDefined).flatten)
      .find(p => p.isDefined).flatten
    res.flatMap(_.headOption)
  }

  //Todo: Later we need to have an option to catch fields for help generation
  def arg(key:String):ConfigValue[String] = addAndReturn((main: ParameterAndOptions, base: ParameterAndOptions, plugin: ParameterAndOptions) => {
    getKey(key)(main,base,plugin)
  })

  def collectArg(key:String):ConfigValue[Seq[String]] = addAndReturn((main: ParameterAndOptions, base: ParameterAndOptions, plugin: ParameterAndOptions) => {
    collectKey(key)(main,base,plugin)
  })

  def opt(key:String):ConfigValue[Boolean] = addAndReturn((main: ParameterAndOptions, base: ParameterAndOptions, plugin: ParameterAndOptions) => {
    getKey(key)(main,base,plugin) match {
      case Some("true") => Some(true)
      case Some("false") => Some(false)
      case _ => None
    }
  })

  def select[T](sel:(String, T)*):ConfigValue[T] = addAndReturn((main: ParameterAndOptions, base: ParameterAndOptions, plugin: ParameterAndOptions) => {
    LazyList.from(sel).flatMap{
      case (key, value) if getKey(key)(main,base,plugin).contains("true") => Some(value)
      case _ => None
    }.headOption
  })

  def param(index:Int):ConfigValue[String] = addAndReturn((main: ParameterAndOptions, base: ParameterAndOptions, plugin: ParameterAndOptions) => {
    LazyList(main, base, plugin).flatMap(_.parameters).drop(index).headOption
  })

  override def configure(main: ParameterAndOptions, base: ParameterAndOptions, plugin: ParameterAndOptions): Unit = {
    values.foreach(_.configure(main, base, plugin))
  }
}
