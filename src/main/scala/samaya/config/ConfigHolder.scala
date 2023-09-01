package samaya.config

import io.circe.Json
import samaya.plugin.PluginManager.{getClass, loadJsonFromResource}

import java.io.{File, FileInputStream}
import java.util.Properties
import scala.util.Using

import scala.jdk.CollectionConverters._


object ConfigHolder {
  private var mainArgs : ParameterAndOptions = null
  private var baseArgs : ParameterAndOptions = null

  def main : ParameterAndOptions = mainArgs
  def base : ParameterAndOptions = baseArgs

  def init(args:Array[String]):Unit = {
    mainArgs = ParameterAndOptions(args)
    val mainClassLoader = getClass.getClassLoader

    val configName = mainArgs.options
      .getOrElse("config-name",Seq.empty)
      .headOption.getOrElse("config.properties")
    val props = new Properties()

    mainArgs.options
      .getOrElse("config", Seq(System.getProperty("user.dir")+File.separator+configName))
      .map(new File(_))
      .filter(f => f.isFile && f.exists())
      .map(new FileInputStream(_))
      .appendedAll(mainClassLoader.getResources(configName).asScala.map(_.openStream()))
      .reverse
      .foreach(Using(_)(props.load))

    baseArgs = ParameterAndOptions(props)
  }



}
