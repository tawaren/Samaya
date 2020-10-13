package samaya.codegen

object NameGenerator {

  val binder = "#"

  private def generateClassified(name:String, classifier:Set[String]): String = {
    classifier.toSeq.sorted.foldLeft(name) {
      case (acc, classifier) => acc + binder + classifier
    }
  }

  val generateInterfaceName: (String, Set[String]) => String = generateClassified
  val generateCodeName: (String, Set[String]) => String = generateClassified

}
