package samaya.plugin.impl.pkg.json

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}

object JsonModel {
  //code is None if it is virtual or compilation failed
  case class Hashes(interface:String, code:Option[String], source:String)
  case class Info(language:String, version:String, classifier:Set[String])

  sealed trait StrongLink {
    def source:Option[String]
    def name:String
    def hash:Hashes
    def info:Info
  }

  case class Component(source:Option[String], name:String, hash:Hashes, info:Info) extends StrongLink
  //Todo: Make Optional - Load over ContentAddress first (derived from Hashes) & Locations as fallback
  //      Give Warning
  case class Locations(interface:String, code:String, source:String)
  case class Package(name:String, hash: String, components: Seq[Component], path:Option[String], locations:Option[Locations], dependencies:Seq[String], includes:Option[Set[String]])
  implicit val codec: JsonValueCodec[Package] = JsonCodecMaker.make[Package](CodecMakerConfig)
}

/*
{
  "name": "MyPackage",
  "comment": "hash is H(H(modules),H(packages.hash) -- Names and locations are transparent",
  "hash":"0x0e...af",
  "modules":[
    {
      "name":"Test",
      "interface":"0x0e...af",
      "code":"0x0e...af",
      "source": "0x0e...af"
    }
  ],

  "locations": {
    "interface": "file://.......",
    "code": "file://.......",
    "source": "file://......."
  },

  "packages": [
    {path:"file://..../package.json",
    "file://..../package.json",
    "file://..../package.json"
  ]

}
* */
