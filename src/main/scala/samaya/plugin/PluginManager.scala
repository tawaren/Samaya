package samaya.plugin

import io.circe._
import io.circe.parser._
import samaya.config.{ConfigHolder, ParameterAndOptions}
import samaya.plugin.config.PluginCompanion

import java.io.{File, FileInputStream, IOException, InputStream}
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
  private var pluginConfig : Json = null

  def init():Unit = {
    val mainClassLoader = getClass.getClassLoader

    val pluginName = ConfigHolder.main.options
      .getOrElse("plugin-config-name",Seq.empty)
      .headOption.getOrElse("plugin.json")

    pluginConfig = (ConfigHolder.base.options ++ ConfigHolder.main.options)
      .getOrElse("plugin-config", Seq(System.getProperty("user.dir")+File.separator+pluginName))
      .map(new File(_))
      .filter(f => f.isFile && f.exists())
      .map(new FileInputStream(_))
      .appendedAll(mainClassLoader.getResources(pluginName).asScala.map(_.openStream()))
      .map(loadJsonFromResource)
      .foldRight(Json.obj()) { (json, acc) =>  acc.deepMerge(json) }
  }

  def loadJsonFromResource(in:InputStream): Json = Using(in){ stream =>
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
              case Some(comp) => comp.configure(ConfigHolder.main, ConfigHolder.base, ParameterAndOptions(args))
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
