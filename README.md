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

```bash
export CLOJARS_USERNAME="XYZ"
export CLOJARS_PASSWORD="XYZ"
```

Create an initial version tag (if you haven't already)

```
git tag v0.1.0
```

Release a new version (tag + deploy):
```
clj -A:release <patch, minor, major>
```

That's it.

To only deploy the current version:

```
clj -A:release
```

To only tag a new version:

```
clj -A:release tag <patch, minor, major>
```
