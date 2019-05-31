package mandalac.plugin.impl.location.relative

import java.io.OutputStream
import java.util.regex.Pattern

import mandalac.plugin.service.{LocationResolver, Selectors}
import mandalac.types.{Identifier, InputSource, Location, Path}

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
      val nameExt = elem.split('.')
      if (nameExt.length > 1) {
        Identifier.Specific(nameExt(0), parts.last.drop(nameExt(0).length + 1))
      } else {
        Identifier.General(nameExt(0))
      }
    })
    Some(Path.Relative(pathIds))
  }



  //Not supported by this plugin on purpose
  override def resolveLocation(parent: Location, path: Path): Option[Location] = ???
  override def resolveSource(parent: Location, path: Path): Option[InputSource] = ???
  override def resolveSink(parent: Location, ident: Identifier.Specific): Option[OutputStream] = ???
  override def listSources(parent: Location): Set[Identifier] = ???
  override def listLocations(parent: Location): Set[Identifier] = ???
  override def provideDefault(): Option[Location] = ???
  override def serializeLocation(parent: Location, target: Location): Option[String] = ???



}
