# deps-lib

![Version Badge](https://img.shields.io/clojars/v/mhuebert/deps-lib)

----

To share a small versioned Clojure library on Clojars should be a simple thing.

This library requires just one small config file, and we take care of creating
the necessary `pom.xml` (using [garamond](https://github.com/workframers/garamond)) and
skinny jar (using [depstar](https://github.com/seancorfield/depstar)), which are then
deployed to clojars (using [deps-deploy](https://github.com/slipset/deps-deploy)).

This library is deployed using itself.

## Usage

Create a `release.edn` file in your project root, eg:

```clj
{:group-id "mhuebert"
 :artifact-id "deps-lib"
 :scm-url "https://github.com/mhuebert/deps-lib"}
```

Add a `:release` alias to your `deps.edn` as follows:

```clj
:aliases
 {:release
  {:extra-deps {mhuebert/deps-lib {:mvn/version "0.1.4"}}
   :main-opts ["-m" "deps-lib.release"]}}
```

Create an initial version tag (if you haven't already)

```
git tag v0.1.0
```

To bump versions:
```
clj -A:release tag <patch, minor, major>
```

To deploy: (requires `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment variables set)
```
clj -A:release
```

