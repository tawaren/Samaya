package samaya.plugin.service

import samaya.plugin.service.category.TaskExecutorPluginCategory
import samaya.plugin.{Plugin, PluginProxy}

import scala.reflect.ClassTag

trait TaskExecutor extends Plugin{
  override type Selector = Selectors.TaskExecutorSelector
  def execute(name:String):Unit
  def apply[S : ClassTag, T : ClassTag](name:String, src:S): Option[T]
}

object TaskExecutor extends TaskExecutor with PluginProxy{

  type PluginType = TaskExecutor
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = TaskExecutorPluginCategory

  class IsClass[U](implicit u: ClassTag[U]){
    def unapply[T](t: ClassTag[T]): Boolean = t == u
    def unapply[T](t: T): Option[U] = t match {
      case u : U => Some(u)
      case _ => None
    }

    def apply[T : ClassTag](u:Option[U]):Option[T] = u match {
      case Some(t : T) => Some(t)
      case _ => None
    }
  }

  object IsClass {
    def apply[U](implicit u: ClassTag[U]):IsClass[U] = new IsClass[U]()
  }

  override def execute(name: String): Unit = {
    select(Selectors.SelectByName(name)).foreach(r => r.execute(name))
  }

  override def apply[S : ClassTag, T : ClassTag](name:String, src:S): Option[T] = {
    select(Selectors.SelectApplyTask(name, implicitly[ClassTag[S]], implicitly[ClassTag[T]])).flatMap(r => r.apply[S,T](name,src))
  }
}



