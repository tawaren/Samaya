package samaya.plugin.impl.inter.json

import samaya.structure.types.{InputSourceId, Location, Region, SourceId}


case class JsonLocation(file:String, elemPath:String) extends Location {
  override def toString: String = s"file $file $localRefString"
  override def localRefString: String = s"path: $elemPath"

  def descendProperty(prop:String):JsonLocation  = JsonLocation(file, s"$elemPath.$prop")
  def decendIndex(pos:Int):JsonLocation = JsonLocation(file, s"$elemPath[$pos]")
}

trait JsonSource {
  def location:JsonLocation
  val src: SourceId = new InputSourceId(Region(location,location))
}