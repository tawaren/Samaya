package samaya.types

import samaya.plugin.service.{AddressResolver, ContentRepositoryLoader, WorkspaceEncoder}

import scala.reflect.ClassTag

trait Repository {
  def resolve[T <: ContentAddressable](address: Address, loader: AddressResolver.ContentLoader[T], extensionFilter:Option[Set[String]] = None): Option[T]
}

object Repository {
  trait AddressableRepository extends Repository with Addressable

  object Loader extends AddressResolver.Loader[AddressableRepository]{
    override def load(src: GeneralSource): Option[AddressableRepository] = ContentRepositoryLoader.loadRepository(src)
    override def tag: ClassTag[AddressableRepository] = implicitly[ClassTag[AddressableRepository]]
  }

  //Thread Safe in Preparation
  private val activeFrame = new InheritableThreadLocal[Set[Repository]]{
    override def initialValue(): Set[Repository] = Set.empty
  }

  def withRepos[T](repos:Set[Repository])(b: => T):T = {
    val oldFrame = activeFrame.get()
    activeFrame.set(oldFrame ++ repos)
    val res:T = b
    activeFrame.set(oldFrame)
    res
  }

  def resolve[T <: ContentAddressable](address: Address, loader: AddressResolver.ContentLoader[T], extensionFilter:Option[Set[String]] = None): Option[T] = {
    Repository.activeFrame.get().to(LazyList).flatMap(_.resolve(address, loader, extensionFilter)).headOption
  }

}
