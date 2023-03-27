package samaya.structure

import samaya.structure.types.{CompLink, Hash}

trait Package {
  def name:String
  def components:Seq[Interface[Component]]
  def findComponents(filter:Package.ComponentFilter): Seq[Interface[Component]] = components.filter(filter.checkMatch)

  def componentByName(name:String):Seq[Interface[Component]] = findComponents(Package.nameFilter(name))
  def componentByLanguage(language:String):Seq[Interface[Component]] = findComponents(Package.languageFilter(language))
  def componentByNameAndClassifier(name:String, classifier:Set[String]):Option[Interface[Component]] = findComponents(Package.preciseFilter(name,classifier)).headOption

  def packageByPath(path:Seq[String]):Option[Package] ={
    if(path.isEmpty) {
      Some(this)
    } else {
      dependencyByName(path.head).flatMap(other => other.packageByPath(path.tail))
    }
  }

  def findComponentByPath(path:Seq[String], filter:Package.ComponentFilter):Seq[Interface[Component]] ={
    packageByPath(path) match {
      case Some(pkg) => pkg.findComponents(filter)
      case None =>Seq.empty
    }
  }

  def componentByPathAndName(path:Seq[String], name:String):Seq[Interface[Component]] = findComponentByPath(path, Package.nameFilter(name))
  def componentByPathAndLanguage(path:Seq[String], language:String):Seq[Interface[Component]] = findComponentByPath(path, Package.languageFilter(language))
  def componentByPathAndNameAndClassifier(path:Seq[String], name:String, classifier:Set[String]):Seq[Interface[Component]] = findComponentByPath(path, Package.preciseFilter(name,classifier))

  private lazy val byCodeHash:Map[Hash,(Package,Interface[Component])] = {
    val directComponentsByHash: Map[Hash, (Package,Interface[Component])] = components.flatMap(m => m.meta.codeHash.map(h => (h,(this,m)))).toMap
    (dependencies.map(_.byCodeHash) :+ directComponentsByHash).reduceLeft((a, b) => a ++ b)
  }

  private lazy val byInterfaceHash:Map[Hash,(Package,Interface[Component])] = {
    val directComponentsByHash: Map[Hash, (Package,Interface[Component])] = components.map(m => (m.meta.interfaceHash, (this,m))).toMap
    (dependencies.map(_.byInterfaceHash) :+ directComponentsByHash).reduceLeft((a, b) => a ++ b)
  }

  def componentByLink(link:CompLink):Option[Interface[Component]] = link match {
    case CompLink.ByCode(hash) =>  byCodeHash.get(hash).map(_._2)
    case CompLink.ByInterface(hash) => byInterfaceHash.get(hash).map(_._2)
  }

  def packageOfLink(link:CompLink):Option[Package] = link match {
    case CompLink.ByCode(hash) =>  byCodeHash.get(hash).map(_._1)
    case CompLink.ByInterface(hash) => byInterfaceHash.get(hash).map(_._1)
  }

  def dependencies:Seq[Package]
  def dependencyByName(name: String): Option[Package] = dependencies.find(p => p.name == name)

}

object Package {
  //todo: add version stuff

  trait ComponentFilter{def checkMatch(m:Interface[Component]):Boolean }

  case class BasicComponentFilter(name:Option[String] = None, language:Option[String] = None, classifier:Set[String] = Set.empty) extends ComponentFilter {
    def checkMatch(m:Interface[Component]):Boolean = {
      (name.isEmpty || m.name == name.get) &&
      (language.isEmpty || m.language == language.get) &&
        classifier.subsetOf(m.classifier)
    }
  }

  def nameFilter(name:String):ComponentFilter = BasicComponentFilter(name = Some(name))
  def languageFilter(language:String):ComponentFilter = BasicComponentFilter(language = Some(language))
  def preciseFilter(name:String, classifier:Set[String]):ComponentFilter = BasicComponentFilter(name = Some(name), classifier = classifier)
  def fullFilter(name:String, language:String, classifier:Set[String]):ComponentFilter = BasicComponentFilter(name = Some(name), language = Some(language), classifier = classifier)

}
