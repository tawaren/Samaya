package samaya.plugin.impl.deps.json

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}

object JsonModel {
  implicit val codec: JsonValueCodec[Seq[String]] = JsonCodecMaker.make[Seq[String]](CodecMakerConfig)
}
