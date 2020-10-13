package samaya.plugin

import java.io.{File, IOException}
import java.net.URLClassLoader

import samaya.plugin.service.PluginCategory

import scala.collection.mutable
import scala.io.Source

object PluginManager {

  private def isJar(file: File): Boolean = {
    val name = file.getName
    val lastIndexOf = name.lastIndexOf(".")
    if (lastIndexOf == -1) return false // empty extension
    name.substring(lastIndexOf) == "jar"
  }

  private val classLoaders = new mutable.HashMap[File,ClassLoader]().withDefault(f => new URLClassLoader(Array(f.toURI.toURL), getClass.getClassLoader))
  private val pluginCache:mutable.Map[PluginCategory[_], Seq[_]] = mutable.HashMap[PluginCategory[_], Seq[_]]()

  private def extractPlugins[T <: Plugin](classLoader:ClassLoader, category: PluginCategory[T]):List[T] = {
    try {
      val pluginInput = classLoader.getResourceAsStream(category.name+".plugin")
      if(pluginInput == null) return List.empty
      val pluginFile = Source.createBufferedSource(pluginInput)
      val lines = pluginFile.getLines()
        lines.flatMap(pluginClass => {
        try {
          val classObj = classLoader.loadClass(pluginClass.trim)
          if (category.interface.isAssignableFrom(classObj)) {
            Some(category.interface.cast(classObj.getDeclaredConstructor().newInstance()))
          } else {
            None
          }
        }catch {
          case e @ (_:ClassNotFoundException | _:ClassCastException) =>
            e.printStackTrace()
            None
        }
      }).toList
    } catch {
      case e: IOException =>
        e.printStackTrace()
        List.empty
    }
  }

  def getPlugins[T <: Plugin](category: PluginCategory[T]):Seq[T] = {
    pluginCache.getOrElseUpdate(category, {
      val directory = new File(".") //todo: config /per category??
      extractPlugins(getClass.getClassLoader, category) ++
        directory.listFiles().filter(f => f.isFile && isJar(f)).flatMap(f => {
          extractPlugins(classLoaders(f), category)
        }).toList
    }).asInstanceOf[Seq[T]]
  }

  def getPlugins[T <: Plugin](category: PluginCategory[T], selector:T#Selector):Seq[T] = {
    getPlugins(category)
      .filter(p =>
        p.matches(
          selector.asInstanceOf[p.Selector]
        )
      )
  }

  def getPlugin[T <: Plugin](category: PluginCategory[T], selector:T#Selector):Option[T] = {
    getPlugins(category)
      .find(p =>
        p.matches(
          selector.asInstanceOf[p.Selector]
        )
      )
  }
}
