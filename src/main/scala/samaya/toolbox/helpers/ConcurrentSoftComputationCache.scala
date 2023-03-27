package samaya.toolbox.helpers

import samaya.toolbox.helpers.ConcurrentSoftComputationCache.SoftCell

import java.lang.ref.SoftReference
import java.util.concurrent.locks.ReentrantLock
import scala.collection.concurrent.TrieMap
import scala.collection.mutable;

class ConcurrentSoftComputationCache[K,V] extends ConcurrentComputationCache[K,V] {
  private val map:mutable.Map[K,SoftCell[V]] = TrieMap.empty[K,SoftCell[V]]
  //Todo: Add a Strong LRI (Least resently incerted) cache
  private def update(k:K,v:V):V = v
  def getOrElseUpdate(k:K)(f : => V):V = map.getOrElseUpdate(k, new SoftCell()).getOrElseUpdate(update(k,f))
}

object ConcurrentSoftComputationCache {
  //Todo: Later we may add a queue and clean the map from time to time of collected entries
  class SoftCell[T](@volatile var ref: SoftReference[T] = null){
    private val lock = new ReentrantLock()
    def getOrElseUpdate(f: => T): T = {
      //LockFree Fast Path
      if(ref != null){
        val res = ref.get()
        if(res != null){
          return res
        }
      }
      //Locked Slow Path
      lock.lock()
      try {
        if(ref == null) {
          val value = f
          ref = new SoftReference(value)
          value
        } else {
          val res = ref.get()
          if(res == null) {
            val value = f
            ref = new SoftReference(value)
            value
          } else {
            res
          }
        }
      } finally {
        lock.unlock()
      }
    }
  }
}
