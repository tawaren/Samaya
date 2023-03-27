package samaya.plugin

import io.circe._
import io.circe.parser._
import samaya.plugin.config.{ParameterAndOptions, PluginCompanion}

import java.io.{File, FileInputStream, IOException, InputStream}
import java.net.URL
import samaya.plugin.service.PluginCategory

import java.nio.charset.StandardCharsets
import java.util.Properties
import scala.collection.concurrent
import scala.io.Source
import scala.reflect.ClassTag
import scala.util.Using
import scala.jdk.CollectionConverters._


object PluginManager {

  private val pluginCache:concurrent.Map[PluginCategory[_], Seq[_]] = concurrent.TrieMap.empty
  private val pattern = """[^\s"]+|"([^"]*)"""".r

  private var mainArgs : ParameterAndOptions = null
  private var baseArgs : ParameterAndOptions = null
  private var pluginConfig : Json = null

  def init(args:Array[String]):ParameterAndOptions = {
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
    val jsonFiles = mainClassLoader.getResources("plugin.json").asScala.map { url =>
      loadJsonFromResource(url)
    }

    pluginConfig = jsonFiles.foldLeft(Json.obj()) { (acc, json) =>
      acc.deepMerge(json)
    }

    mainArgs
  }

  def loadJsonFromResource(url:URL): Json = Using(url.openStream()){ stream =>
    val jsonStr: String = Source.fromInputStream(stream, StandardCharsets.UTF_8.name).mkString
    parse(jsonStr).getOrElse(Json.Null)
  }.get

  private def getCompanion(pluginClass:String):Option[PluginCompanion] = {
    try {
      val companionClazz = Class.forName(pluginClass + "$")
      val companionField = companionClazz.getField("MODULE$")
      Some(companionField.get(null).asInstanceOf[PluginCompanion])
    } catch {
      case _:NoSuchFieldException
           | _ : ClassCastException
           | _ : ClassNotFoundException => None
    }
  }

  private def extractPlugins[T <: Plugin](category: PluginCategory[T]):Seq[T] = {
    try {
      (pluginConfig \\ category.name)
        .flatMap(_.asObject)
        .map(_.toMap)
        .flatMap(_.filter(!_._1.startsWith("#")).flatMap{
          case (pluginClass, pluginArgs) => try {
            val comp = getCompanion(pluginClass)
            val args = pluginArgs.asObject.map(_.toMap).getOrElse(Map.empty)
            comp match {
              case Some(comp) => comp.configure(mainArgs, baseArgs, ParameterAndOptions(args))
              case None =>
            }
            val classObj = Class.forName(pluginClass)
            if (category.interface.isAssignableFrom(classObj)) {
              val inst = comp match {
                case Some(comp) => comp.init(classObj)
                case None => classObj.getDeclaredConstructor().newInstance()
              }
              Some(category.interface.cast(inst))
            } else {
              None
            }
          } catch {
            case e@(_: ClassNotFoundException | _: ClassCastException) =>
              e.printStackTrace()
              None
          }
        })
    } catch {
      case e: IOException =>
        e.printStackTrace()
        Seq.empty
    }
  }

  def getPlugins[T <: Plugin : ClassTag](category: PluginCategory[T]):Seq[T] = {
    pluginCache.getOrElseUpdate(category, synchronized {
      extractPlugins(category)
    }).asInstanceOf[Seq[T]]
  }

  def getPlugins[T <: Plugin : ClassTag](category: PluginCategory[T], selector:T#Selector):Seq[T] = {
    getPlugins(category)
      .filter(p =>
        p.matches(
          selector.asInstanceOf[p.Selector]
        )
      )
  }

  def getPlugin[T <: Plugin : ClassTag](category: PluginCategory[T], selector:T#Selector):Option[T] = {
    getPlugins(category)
      .find(p =>
        p.matches(
          selector.asInstanceOf[p.Selector]
        )
      )
  }
}
