# deps-library

![Version Badge](https://img.shields.io/clojars/v/mhuebert/deps-library)

----

To publish a small Clojure library to Clojars should be a simple thing.

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
{:group-id "mhuebert"
 :artifact-id "deps-library"
 :scm-url "https://github.com/mhuebert/deps-library"}
```

Add a `:release` alias to your `deps.edn` as follows:

```clj
:aliases
 {:release
  {:extra-deps {mhuebert/deps-library {:mvn/version "VERSION"}}
   :main-opts ["-m" "deps-library.release"]}}
```

Make sure `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment variables are set.
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
clj -A:release patch # patch, minor, or major
```

That's it.

To only release the current version (pom + jar + deploy):

```sh
clj -A:release
```

To only tag a new version:

```sh
clj -A:release tag patch # patch, minor, or major
```

## Rationale

In my experience, [tools.deps](https://github.com/clojure/tools.deps.alpha) has been a big step
forward for Clojure dependency handling. In particular, "git deps" make it easy to consume small
libraries without any extra effort. However, for production code we often want to depend only on
pinned versions that are stored on reliable, public, immutable repositories like Clojars
rather than rely on GitHub repositories, which are more easily moved/deleted.

There are four distinct steps in the release process (tag, pom, jar, deploy). In isolation, each step
is already adequately covered by a number of different tools. However, tying them all together is
enough of a pain (~one page of code, understanding & configuring each tool) that it discourages versioned
releases of small libraries. `deps-library` should make the process relatively painless.
