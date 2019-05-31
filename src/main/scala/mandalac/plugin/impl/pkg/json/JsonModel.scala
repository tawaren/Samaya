package mandalac.plugin.impl.pkg.json

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}

object JsonModel {
  case class Hashes(interface:String, code:String, source:String)
  case class Info(language:String, version:String, classifier:String)
  case class Module(name:String, hash: Hashes, info:Info)
  case class Locations(interface:String, code:String, source:String)
  case class Package(name:String, hash: String, modules: Seq[Module], locations:Locations, dependencies:Seq[String])
  implicit val codec: JsonValueCodec[Package] = JsonCodecMaker.make[Package](CodecMakerConfig())

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