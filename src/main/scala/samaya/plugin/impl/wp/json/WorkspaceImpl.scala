package samaya.plugin.impl.wp.json

import samaya.structure.LinkablePackage
import samaya.types.{Location, Path, Workspace}

class WorkspaceImpl(
                     override val name: String,
                     override val workspaceLocation: Location,
                     override val includes: Option[Set[Workspace]],
                     override val dependencies: Option[Set[LinkablePackage]],
                     override val sources: Option[Set[Path]],
                     override val sourceLocation: Location,
                     override val codeLocation: Location,
                     override val interfaceLocation: Location
) extends Workspace