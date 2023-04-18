package samaya.toolbox.helpers

trait ConcurrentComputationCache[K,V] {
  def getOrElseUpdate(k:K)(f : => V):V
  def forcedUpdate(k: K, v: V): Unit
}