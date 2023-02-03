package samaya.plugin.impl.wp.json

import samaya.structure.LinkablePackage
import samaya.types.{Directory, Address, Workspace}

class WorkspaceImpl(
                     override val name: String,
                     override val workspaceLocation: Directory,
                     override val includes: Option[Set[Workspace]],
                     override val dependencies: Option[Set[LinkablePackage]],
                     override val sources: Option[Set[Address]],
                     override val sourceLocation: Directory,
                     override val codeLocation: Directory,
                     override val interfaceLocation: Directory
) extends Workspace