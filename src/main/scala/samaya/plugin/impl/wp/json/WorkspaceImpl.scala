package samaya.plugin.impl.wp.json

import samaya.structure.LinkablePackage
import samaya.types.{Address, Directory, Repository, Workspace}

class WorkspaceImpl(
                     override val name: String,
                     override val location: Directory,
                     override val packageTarget: Directory,
                     override val includes: Set[Workspace],
                     override val repositories: Set[Repository],
                     override val dependencies: Set[LinkablePackage],
                     override val sources: Set[Address],
                     override val sourceLocation: Directory,
                     override val codeLocation: Directory,
                     override val interfaceLocation: Directory
) extends Workspace