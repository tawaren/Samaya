package mandalac.plugin.impl.wp.json

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}

object JsonModel {
  case class Locations(interface:String, code:String, source:String)
  case class Workspace(name:String, locations:Locations, includes:Option[Seq[String]], dependencies:Option[Seq[String]], modules:Option[Seq[String]])
  implicit val codec: JsonValueCodec[Workspace] = JsonCodecMaker.make[Workspace](CodecMakerConfig())

}