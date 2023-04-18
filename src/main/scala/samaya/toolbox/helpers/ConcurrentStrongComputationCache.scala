package samaya.toolbox.helpers

import samaya.toolbox.helpers.ConcurrentStrongComputationCache.StrongCell

import java.util.concurrent.locks.ReentrantLock
import scala.collection.concurrent.TrieMap
import scala.collection.mutable;

class ConcurrentStrongComputationCache[K,V] extends ConcurrentComputationCache[K,V] {
  private val map:mutable.Map[K,StrongCell[V]] = TrieMap.empty[K,StrongCell[V]]
  private def update(k:K,v:V):V = v
  //Note: We do not just forward to map.getOrElseUpdate
  //        because we want to make sure that a
  //        Virtual Thread friendly implementation is used
  override def getOrElseUpdate(k:K)(f : => V):V = map.getOrElseUpdate(k, new StrongCell[V]()).getOrElseUpdate(update(k,f))
  override def forcedUpdate(k: K, v: V): Unit = map.put(k, new StrongCell[V](Some(v)))
}



object ConcurrentStrongComputationCache {
  //Todo: Later we may add a queue and clean the map from time to time of collected entries
  class StrongCell[T](@volatile var ref: Option[T] = None){
    private val lock = new ReentrantLock()
    def getOrElseUpdate(f: => T): T = {
      //LockFree Fast Path
      if(ref.isDefined){
        ref.get
      } else {
        //Locked Slow Path
        lock.lock()
        try {
          if(ref.isEmpty) ref = Some(f)
          ref.get
        } finally {
          lock.unlock()
        }
      }
    }
  }
}



