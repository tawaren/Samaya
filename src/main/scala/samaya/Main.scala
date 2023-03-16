package samaya

import samaya.build.{BuildTool, CleanTool}
import samaya.deploy.DeployTool
import samaya.validation.ValidateTool

object Main {
  def main(args: Array[String]): Unit = {
    if(args.length == 0) {
        println("to few parameters")
    } else{
      args(0) match {
        case "build" => BuildTool.main(args.drop(1))
        case "clean" => CleanTool.main(args.drop(1))
        case "validate" => ValidateTool.main(args.drop(1))
        case "deploy" => DeployTool.main(args.drop(1))
      }
    }
  }
}
