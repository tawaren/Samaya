package samaya.plugin.shared.repositories

import samaya.plugin.service.AddressResolver
import samaya.structure.ContentAddressable
import samaya.types.Address

import scala.collection.mutable

object Repositories {

  val active_repos: mutable.Set[Repository] = mutable.Set.empty

  //Note the content addressing is in relation to the indexed hashes not the values
  //This allows moving content around without breaking the repository
  trait Repository extends ContentAddressable {
    private var installed = 0;

    def isInstalled:Boolean = installed > 0

    def install():Unit = {
      if(installed == 0){
        active_repos.add(this)
      }
      installed+=1;
    }

    def uninstall():Unit = {
      if(installed > 0){
        installed-=1;
        if(installed == 0){
          active_repos.remove(this)
        }
      }
    }

    def resolve[T <: ContentAddressable](address:Address, loader: AddressResolver.Loader[T]): Option[T]
  }
}
