package samaya.plugin.impl.location.relative

import samaya.plugin.service.{LocationResolver, Selectors}
import samaya.types.{Identifier, InputSource, Location, OutputTarget, Path}

class RelativeLocationParser extends LocationResolver{

  override def matches(s: Selectors.LocationSelector): Boolean = s match {
    //we can not parse absolutes for a specific protocoll
    case Selectors.Parse(LocationResolver.protocol(_ , _)) => false
    //we can parse non absolutes (not for a protocoll)
    case Selectors.Parse(_) => true
    //we do only parse
    case _ => false
  }

  override def parsePath(path: String): Option[Path] =  {
    val parts = LocationResolver.pathSeparator.split(path)
    val pathIds = parts.map(elem => {
      if(elem == "..") {
        Identifier.General("..")
      } else {
        val nameExt = elem.split('.')
        if (nameExt.length > 1) {
          Identifier.Specific(nameExt(0), parts.last.drop(nameExt(0).length + 1))
        } else {
          Identifier.General(nameExt(0))
        }
      }
    })
    Some(Path.Relative(pathIds))
  }



  //Not supported by this plugin on purpose
  override def resolveLocation(parent: Location, path: Path): Option[Location] = ???
  override def resolveSource(parent: Location, path: Path,  extensionFilter:Option[Set[String]] = None): Option[InputSource] = ???
  override def resolveSink(parent: Location, ident: Identifier.Specific): Option[OutputTarget] = ???
  override def listSources(parent: Location): Set[Identifier] = ???
  override def listLocations(parent: Location): Set[Identifier] = ???
  override def provideDefault(): Option[Location] = ???
  override def serialize(parent: Location, target: Location, resourceName:Option[String]): Option[String] = ???



}
