package samaya.toolbox.stack

import scala.language.higherKinds


trait Merger[K[_]] {
  def merge[V](k:K[V], vs:Seq[V]):Option[V]
}

class KeyTypedMap[K[_]] private (map: Map[Any,Any]) {
    def updated[V](k:K[V],v:V):KeyTypedMap[K] = new KeyTypedMap(map.updated(k,v))
    def get[V](k:K[V]):Option[V] = map.get(k).asInstanceOf[Option[V]]
    def removed[V](k:K[V]):KeyTypedMap[K] = new KeyTypedMap(map - k)
    def keySet():Set[K[_]] = map.keySet.asInstanceOf[Set[K[_]]]
    override def toString: String = map.toString
}

object KeyTypedMap {
  def empty[K[_]]():KeyTypedMap[K] = new KeyTypedMap(Map.empty)
  def mergeAll[K[_]](maps:Seq[KeyTypedMap[K]], merger:Merger[K]):KeyTypedMap[K] = {
    val keys = maps.flatMap(_.keySet())
    new KeyTypedMap(keys.flatMap(d => merger.merge(d.asInstanceOf[K[Any]],maps.flatMap(_.get(d))).map((d, _))).toMap)
  }
}
