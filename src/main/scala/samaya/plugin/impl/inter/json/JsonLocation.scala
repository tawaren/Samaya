package samaya.plugin.impl.inter.json

import samaya.structure.types.{InputSourceId, Location, Region, SourceId}
import samaya.types.ContentAddressable


case class JsonLocation(source:ContentAddressable, elemPath:String) extends Location {
  override def toString: String = s"${source.identifier.fullName} @ $localRefString - ${source.location}"
  override def localRefString: String = s"$elemPath"

  def descendProperty(prop:String):JsonLocation  = JsonLocation(source, s"$elemPath.$prop")
  def decendIndex(pos:Int):JsonLocation = JsonLocation(source, s"$elemPath[$pos]")
}

trait JsonSource {
  def location:JsonLocation
  val src: SourceId = new InputSourceId(Region(location,location))
}