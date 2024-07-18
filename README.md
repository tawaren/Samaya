
# Samaya
A build tool for compiling and deploying smart contract for the [Sanskrit platform](https://github.com/tawaren/Sanskrit)   
This is part of my PhD thesis at the University of Zurich.

## License

Copyright (C) 2024 Markus Knecht, System Communication Group, University of Zurich.

This project is licensed under the GNU General Public License v3.0. You can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

## Status

**Research Grade Software**

This software is currently in a research-grade state and is not production-ready. It has been developed for testing and evaluation purposes. While it includes all necessary components for these purposes, additional work is required to make it suitable for production environments. Especially, improved error reporting and integration of central code repositories.

**Use at Your Own Risk**

Use of this software is at your own risk. The developers and contributors are not responsible for any damage or loss that may result from using this software. It is provided "as-is" without any warranties or guarantees.

## What is the purpose of the Samaya build tool
The Samaya build tool provides the means to produce Sanskrit byte code and deploy it to the [Sanskrit platform](https://github.com/tawaren/Sanskrit).  
In the process it leverages unique properties of Sanskrit, especially the fact that code is content addressed (meaning code is identified over the hash of its content).  
The integrity of code dependencies is essential when developing smart contracts, as flaws and vulnerabilities can lead to large monetary loses.  
To prevent this smart contract developers often use local copies of code they depend on, instead of relying on established dependency management techniques.   
In Samaya this is not necessary as it can refer to code over content-addresses which it verifies after fetching the code from a repository.  
Further, it fetches the corresponding sources and verifies that they compile to the fetched code, allowing developers to inspect their dependencies.  
This gives the benefit of established dependency management techniques and code sharing combined with security properties that surpass those of manually coping code locally.  
As an example if a developer get the content-address of a dependency from a trusted source their is no need to inspect the code.  
Currently the supported repositories to fetch code from are limited to .sar files (Samaya archive files) and .repo files (Samaya repository index). But these can be supplied from an http request and do not need to be local.

Further, Samaya includes a compiler for Mandala, a high level smart contract language designed for the [Sanskrit platform](https://github.com/tawaren/Sanskrit).

## How is the Samaya build tool structured

Samaya uses workspaces to structure source code and packages for compiled code.

A workspace has the following content:
- The name of the workspace
- A list of source files contained in the workspace (location-addressed)
- A list of included sub-workspaces (location-addressed)
- A list of imported packages (content-addressed)
- A list of repositories (location-addressed) to use when resolving content-addresses
- The location addresses to where the artifacts resulting from the build shall be placed.

A package has the following content:
- The name of the package
- A list of components contained in the package.
  - A component has:
    - A name
    - A Sanskrit byte code file (content-addressed) [Optional: as not all source file produce code]
- An interface file (content-addressed)
  - A source code file (content-addressed)
  - Optional location-addresses for the Source code file
  - Meta information (like type: Module, Class, ...)
- A list of included sub-packages (content-addressed)
- A list of imported packages (content-addressed)

Compiling a workspace (and its sub-workspaces) results in a package (and sub-packages).  
The source files are compiled to components.

The encoding format of packages and workspaces is open and provided over plugins.  
However, a json format is provided by default.

Further, for workspaces a directory tree encoding is supported. This allows to interpret a folder as workspace with the following mapping:
- The folder name is the workspace name
- Sub folders in the folder become sub-workspaces
- Source code files in the folder become the contained sources
- Samaya archives (.sar) files in the folder become imported packages and used repositories
- Package files in the folder become imported packages
- Entries in .deps files become imported packages
- Repository (.repo) files and Samaya archives (.sar) files in the folder become repositories for resolving content addresses
- Entries in .reps files become used repositories
- The output folders are set to build/packages

This make development easy to add a dependency, simply copy an encoded package file, a code archive into a folder, or add its content-address to a .deps file.  
Same goes for code repository over .repo, .reps, and .sar files.  
However, if preferred writing a workspace explicitly as .json file works as well.

Samaya supports many plugins to add:
- new programming languages (default: Mandala)
- new encoding formats (default: json, directory tree)
- new repository types for content-address resolution (default: .repo files [Note .sar files contain a .repo file])
- new address resolvers for location-address resolution (default: file & http protocol & .sar, .zip and .jar archive formats)
- new deployers for other blockchain targets (default: sanskrit local test server)

## What is the goal of the Mandala programming language

Mandala is a high level language for Sanskrit. Most features are a one to one translation of corresponding Sanskrit concepts.  However, Mandala adds convinience features like type inference and copy drop inference (needed for sub structural type handling)

Their are some extra features like classes and implementation which work similar to type classes and instances in other functional programming languages.

For an indepth explanation of the Mandala syntax and Semantics consult the PhD thesis (the link follows after publication).  For code examples take a look into the [Mandala-Libs-And-Examples](https://github.com/tawaren/Mandala-Libs-And-Examples) repository.

## Build and Use Guide

Use the following command to build the Samaya build tool:    
```./gradlew build```

Use the following command to build a Samaya workspace:    
```./samaya build <Path to workspace>```

Use the following command to create a Samaya archive (.sar file) package from a workspace or package:    
```./samaya package <Path to workspace or package>```

Use the following command to run validation checks on a package or archive:    
```./samaya validate <Path to package or archive>```

Use the following command to deploy a workspace, a package or a archive to the sanskrit local test server:    
```./samaya deploy <Path to workspace, package, or archive>```