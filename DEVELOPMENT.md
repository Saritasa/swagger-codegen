# Development of fork

## Workflow

Each minor version is separate branch while patch version should be tags. In other words if bring a big change
create a separate branch, but if you want to patch things, just make commit and tag it.

## How ot test

To check your wor you can build codegen via docker like this

```sh
docker build --file DockerfileSwaggerCodegen --tag=saritasa-swagger-codegen:<version> .
```

Then you can test it like this

```sh
docker run --rm --volume $PWD:/local saritasa-swagger-codegen:2.4.0 generate -i scheme.yaml --lang=swift4 --output=/local/ --config=/local/config.json
```

After that run this to check that there are no errors in code

```sh
pod lib lint --allow-warnings
```
