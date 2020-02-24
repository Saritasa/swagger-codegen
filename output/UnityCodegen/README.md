# Swagger Codegen for the UnityCodegen library

## Overview
This code gen tool generates DTO and DTO-Test files.
Test files will not compile with specific library which is not shipped with library. (TODO: Add in future)

Once modified, you can run this:

```
mvn package
```

In your generator project.  A single jar file will be produced in `target`. You can now use that with codegen:

```
#!/bin/sh

java -cp 'output/UnityCodegen/target/UnityCodegen-swagger-codegen-1.0.0.jar;modules/swagger-codegen-cli/target/swagger-codegen-cli.jar' io.swagger.codegen.v3.cli.SwaggerCodegen generate \
-l UnityCodegen \
-c config.json \
-i some.yaml \
-o ret
```

config.json example:
```
{
  "packageName": "YourProject.API",
  "packageVersion": "1.0.0",
  "apiModelsPath": "YourProject/Api/Models/"
}
```

Now your templates are available to the client generator and you can write output values

## Important
Current version of swagger cli has bug - example values for format: date result in wrong format. Workaround: do not use
examples for date format. Leave it blank. Otherwise - tests could be red

## But how do I modify this?
The `UnitycodegenGenerator.java` has comments in it--lots of comments.  There is no good substitute
for reading the code more, though.  See how the `UnitycodegenGenerator` implements `DefaultCodegenConfig`.
That class has the signature of all values that can be overridden.

For the templates themselves, you have a number of values available to you for generation.
You can execute the `java` command from above while passing different debug flags to show
the object you have available during client generation:

```
# The following additional debug options are available for all codegen targets:
# -DdebugSwagger prints the OpenAPI Specification as interpreted by the codegen
# -DdebugModels prints models passed to the template engine
# -DdebugSupportingFiles prints additional data passed to the template engine
```

