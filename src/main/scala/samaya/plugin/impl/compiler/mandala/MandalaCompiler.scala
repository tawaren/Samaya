package samaya.plugin.impl.compiler.mandala

import org.antlr.v4.runtime.{CharStreams, CommonTokenStream}
import samaya.build.jobs.Dependency
import samaya.compilation.ErrorManager.{newPlainErrorScope, producesErrorValue}
import samaya.plugin.impl.compiler.common.BasicErrorListener
import samaya.plugin.impl.compiler.mandala.MandalaCompiler.parsingCache
import samaya.plugin.impl.compiler.mandala.compiler.MandalaBaseCompiler
import samaya.plugin.impl.compiler.mandala.process.{ImplicitInjector, InstanceFinder, TypeAndClassInference}
import samaya.plugin.service.Selectors.{CompilerSelectorByIdentifier, CompilerSelectorByMeta, CompilerSelectorBySource}
import samaya.plugin.service.{LanguageCompiler, Selectors}
import samaya.structure.{Component, Interface, Package}
import samaya.toolbox.helpers.{ConcurrentComputationCache, ConcurrentSoftComputationCache}
import samaya.toolbox.process.{CaseSorter, CopyDiscardInjector, RollbackFiller}
import samaya.toolbox.transform.EntryTransformer
import samaya.types.InputSource


object MandalaCompiler {
  val Language:String = "mandala"
  val Version:String = "0"
  private val BaseClassifiers:Set[String]  = Set(Language)

  val MandalaModule_Classifier:Set[String] = BaseClassifiers ++ Set("linkable", "mandala_module")
  val FunClass_Classifier:Set[String] = BaseClassifiers ++ Set("class", "functions")
  val SigClass_Classifier:Set[String] = BaseClassifiers ++ Set("class", "signatures")
  val DefInstance_Classifier:Set[String]  = BaseClassifiers ++ Set("instance", "definitions")
    //We can use standard Serializer deserializer for these so we add the corresponding classifier
  val ImplInstance_Classifier:Set[String]  = BaseClassifiers ++ Set(Component.MODULE_CLASSIFIER, "instance", "implements")
  val Transaction_Classifier:Set[String]  = BaseClassifiers ++ Set(Component.TRANSACTION_CLASSIFIER)

  val Implicit_Attribute_Name = "implicit"
  val Context_Attribute_Name = "context"
  val Aliasing_Module_Attribute_Name = "aliased_in"

  //Todo: Move into class and make configurable
  val parsingCache = new ConcurrentSoftComputationCache[InputSource,Option[MandalaParser.FileContext]]()
}

class MandalaCompiler extends LanguageCompiler{
  override def matches(s: Selectors.CompilerSelector): Boolean = s match {
    case CompilerSelectorByMeta(MandalaCompiler.Language, MandalaCompiler.Version, classifier) => MandalaCompiler.BaseClassifiers.subsetOf(classifier)
    case CompilerSelectorBySource(source) => source.identifier.extension.contains(MandalaCompiler.Language)
    case CompilerSelectorByIdentifier(identifier) => identifier.extension.contains(MandalaCompiler.Language)
    case _ => false
  }

  private def parseFile(source: InputSource):Option[MandalaParser.FileContext] = parsingCache.getOrElseUpdate(source) {
    producesErrorValue {
      source.read{ input =>
        val lexer =  new MandalaLexer(CharStreams.fromStream(input))
        val errorHandler = new BasicErrorListener(source)
        lexer.removeErrorListeners()
        lexer.addErrorListener(errorHandler)
        val tokens = new CommonTokenStream(lexer)
        val parser = new MandalaParser(tokens)
        parser.removeErrorListeners()
        parser.addErrorListener(errorHandler)
        parser.file()
      }
    }
  }

  override def compileAndBuildFully(source: InputSource, pkg: Package)(buildFunction: Component => (Package, Option[Interface[Component]])): Package  = {
    val env = new Environment(source, pkg) {
      override def createPipeline(): EntryTransformer = {
        val instanceFinder = new InstanceFinder(globalInstances, localInstances)
        CaseSorter.andThen(
          new TypeAndClassInference(instanceFinder)
        ).andThen(
          new ImplicitInjector(instanceFinder)
        ).andThen(
          CopyDiscardInjector
        ).andThen(
          RollbackFiller
        )
      }

      override def buildComponent(comp: Component): (Package, Option[Interface[Component]]) = {
        buildFunction(comp)
      }
    }

    val vis = new MandalaBaseCompiler(env)
    newPlainErrorScope {
      parseFile(source) match {
        case Some(file) => vis.visitFile(file)
        case None =>
      }
    }
    env.pkg
  }

  override def extractDependencies(source: InputSource): Set[Dependency] = newPlainErrorScope {
    parseFile(source).toSet.flatMap((tree: MandalaParser.FileContext) => {
      val vis = new MandalaDependencyExtractorVisitor(source)
      vis.visitFile(tree).map{
        case (k,v) => Dependency(k,v)
      }
    })
  }

  override def extractComponentNames(source: InputSource): Set[String] = newPlainErrorScope {
    parseFile(source).toSet.flatMap((tree: MandalaParser.FileContext) => {
      val vis = new NameExtractorVisitor()
      vis.visitFile(tree)
    })
  }
}
