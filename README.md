# deps-lib

![Version Badge](https://img.shields.io/clojars/v/mhuebert/deps-lib)

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
  {:extra-deps {mhuebert/deps-lib {:local/root "../deps-lib"}}
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