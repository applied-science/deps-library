# deps-library

![Version Badge](https://img.shields.io/clojars/v/applied-science/deps-library)

To publish a small Clojure library to Clojars should be a simple thing.

----

As a library author, my needs are simple:

1. Bump the version when ready for a release
2. Ship the code to Clojars

With `deps-library` this is possible using just one small config file. From that we
create a `pom.xml` (using [garamond](https://github.com/workframers/garamond))
and thin jar (using [depstar](https://github.com/seancorfield/depstar)), and
deploy to clojars (using [deps-deploy](https://github.com/slipset/deps-deploy)).

This library is deployed using itself.

## Usage

Create a `release.edn` file in your project root, eg:

```clj
{:group-id "applied-science"
 :artifact-id "deps-library"
 :scm-url "https://github.com/applied-science/deps-library"}
```

Add a `:release` alias to your `deps.edn` as follows:

```clj
:aliases
 {:release
  {:extra-deps {applied-science/deps-library {:mvn/version "VERSION"}}
   :main-opts ["-m" "applied-science.deps-library"]}}
```

Make sure `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment variables are set
(unless you are passing in `--clojars-username` and `--clojars-password` directly).
For example, add the following to your `~/.bashrc` or `~/.zshrc` or equivalent:

```sh
export CLOJARS_USERNAME="XYZ"
export CLOJARS_PASSWORD="XYZ"
```

Create an initial version tag (if you haven't already)

```sh
git tag v0.1.0
```

Release a new version (tag + pom + jar + deploy):

```sh
clj -A:release --patch # patch, minor, or major
```

That's it.

To release the current version (pom + jar + deploy):

```sh
clj -A:release
```

To just tag a new version:

```sh
clj -A:release tag --patch # patch, minor, or major
```

## Rationale

[tools.deps](https://github.com/clojure/tools.deps.alpha) brought "git deps" to Clojure, making it
easy to consume small libraries without any extra effort. However, for production code we often want
to depend only on pinned versions that are stored on reliable, public, immutable repositories like
Clojars rather than rely on GitHub repositories, which are more easily moved/deleted.

There are four distinct steps in the release process (tag, pom, jar, deploy). In isolation, each step
is already adequately covered by a number of different tools. However, tying them all together is
enough of a pain (~one page of code, understanding & configuring each tool) that it discourages versioned
releases of small libraries. `deps-library` should make the process relatively painless.

## Versioning

By default (via [garamond](https://github.com/workframers/garamond)), versions are managed
via git tags and are not stored in source code. This means we can release new versions without making any
extra commits to update version numbers. Versions created this way can be easily browsed on GitHub
(be sure to push with `--follow-tags`) and major CI services are easily configured to run workflows triggered
by tagged commits.

Alternatively, you can pass a `--version` argument at the command line, or add a `:version` to your
`release.edn` file (in which case you must handle incrementing the version yourself).

## CLI

### Commands

eg. `clj -A:release <command> <...options>`

Core flow:

- _default_ (no command) runs tag + pom + jar + deploy
- **tag** - creates git tag. if an increment is specified, increments the version first.
- **pom** - creates pom.xml file
- **jar** - creates thin jar
- **deploy** - deploys to clojars

Other commands:

- **install** - install version to local maven repo (runs tag + pom + jar)
- **version** - prints version according to given options/environment

### Options

The `release.edn` file itself is optional - all config can be also be passed in via the command line:

```
  -v, --version VERSION                                          Specify a fixed version
  -i, --incr INCREMENT                                           Increment the current version
      --skip-tag                                                 Do not create a git tag for this version
      --prefix PREFIX                      v                     Version prefix for git tag
      --patch                                                    Increment patch version
      --minor                                                    Increment minor version
      --major                                                    Increment major version
      --config CONFIG                      release.edn           Path to EDN options file
      --group-id GROUP-ID
      --artifact-id ARTIFACT-ID
      --scm-url SCM-URL                                          The source control management URL (eg. github url)
      --clojars-username CLOJARS-USERNAME  environment variable  Your Clojars username
      --clojars-password CLOJARS-PASSWORD  environment variable  Your Clojars password
      --dry-run                                                  Print expected actions, avoiding any side effects
  -h, --help                                                     Print CLI options

```

All command-line args are also valid keys for inclusion in `release.edn` (except `--config` for obvious reasons).