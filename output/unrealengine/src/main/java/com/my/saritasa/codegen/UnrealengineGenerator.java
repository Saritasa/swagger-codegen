package com.my.saritasa.codegen;

import io.swagger.codegen.*;
import io.swagger.models.properties.*;
import io.swagger.models.Model;
import java.util.*;
import java.io.File;

public class UnrealengineGenerator extends DefaultCodegen implements CodegenConfig {

    public static final String DEFAULT_INCLUDE = "defaultInclude";

    // source folder where to write the files
    protected String apiVersion = "1.0.0";
    protected String defaultInclude = "";

    /**
     * Configures the type of generator.
     *
     * @return  the CodegenType for this generator
     * @see     io.swagger.codegen.CodegenType
     */
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    /**
     * Configures a friendly name for the generator.  This will be used by the generator
     * to select the library with the -l flag.
     *
     * @return the friendly name for the generator
     */
    public String getName() {
        return "unrealengine";
    }

    /**
     * Returns human-friendly help for the generator.  Provide the consumer with help
     * tips, parameters here
     *
     * @return A string value for the help message
     */
    public String getHelp() {
        return "Generates a unrealengine client library.";
    }

    public UnrealengineGenerator() {
        super();

        /**
         * Model Package.  Optional, if needed, this can be used in templates
         */
        modelPackage = "io.swagger.client.model";

        // set the output folder here
        outputFolder = "api";

        /**
         * Models.  You can write model files using the modelTemplateFiles map.
         * if you want to create one template for file, you can do so here.
         * for multiple files for model, just put another entry in the `modelTemplateFiles` with
         * a different extension
         */
        modelTemplateFiles.put("model-header.mustache", ".h");
        modelTemplateFiles.put("model-source.mustache", ".cpp");

        modelTestTemplateFiles.put("model-test-header.mustache", ".hpp");

        cliOptions.clear();

        // CLI options
        addOption(CodegenConstants.MODEL_PACKAGE, "C++ namespace for models (convention: name.space.model).",
                this.modelPackage);
        addOption(DEFAULT_INCLUDE,
                "The default include statement that should be placed in all headers for including things like the declspec (convention: #include \"Commons.h\" ",
                this.defaultInclude);

        /**
         * Template Location.  This is the location which templates will be read from.  The generator
         * will use the resource stream to attempt to read the templates.
         */
        templateDir = "unrealengine";

        languageSpecificPrimitives = new HashSet<String>(
                Arrays.asList("int", "char", "bool", "long", "float", "double", "int32", "int64"));

        typeMapping = new HashMap<String, String>();
        typeMapping.put("date", "FDateTime");
        typeMapping.put("DateTime", "FDateTime");
        typeMapping.put("string", "FString");
        typeMapping.put("integer", "int32");
        // JSON does not actually support int64 as well as JavaScript.
        // There is not much sense to send int64 via API since some clients can't handle them
        typeMapping.put("long", "double");
        typeMapping.put("boolean", "bool");
        typeMapping.put("array", "TArray");
        typeMapping.put("map", "_notsupported_"); // @@
        typeMapping.put("file", "FString");
        // Use void* object is not defined
        typeMapping.put("object", "void*");
        typeMapping.put("binary", "FString");
        typeMapping.put("number", "double");
        typeMapping.put("float", "double");

        /**
         * Reserved words.  Override this with reserved words specific to your language
         */
        reservedWords = new HashSet<String> (
                Arrays.asList(
                        "FString",
                        "FDateTime",
                        "TArray",
                        "TMap")
                // @@ to be contunued
        );

        /**
         * Additional Properties.  These values can be passed to the templates and
         * are available in models, apis, and supporting files
         */
        additionalProperties.put("apiVersion", apiVersion);

        super.importMapping = new HashMap<String, String>();
        importMapping.put("FString", "");
        importMapping.put("TArray", "");
        importMapping.put("FDateTime", "");
        importMapping.put("TOptional", "");
    }

    @Override
    public Map<String, Object> postProcessAllModels(Map<String, Object> objs) {

        // Index all CodegenModels by model name.
        Map<String, CodegenModel> allModels = new HashMap<String, CodegenModel>();
        for (Map.Entry<String, Object> entry : objs.entrySet()) {
            String modelName = toModelName(entry.getKey());
            Map<String, Object> inner = (Map<String, Object>) entry.getValue();
            List<Map<String, Object>> models = (List<Map<String, Object>>) inner.get("models");
            for (Map<String, Object> mo : models) {
                CodegenModel cm = (CodegenModel) mo.get("model");
                allModels.put(modelName, cm);
            }
        }

        // It is not possible to set additional parameters when use json $ref
        // Details: https://github.com/OAI/OpenAPI-Specification/issues/241
        // Take vendorExtensions from referenced model
        for (CodegenModel cm : allModels.values()) {
            if (cm.vars == null)
                continue;
            for (CodegenProperty cp : cm.vars) {
                if (cp.isPrimitiveType)
                    continue;

                CodegenModel refModel = allModels.get(cp.datatype);
                if (refModel != null) {
                    Map<String, Object> tmp = new HashMap<String, Object>();
                    tmp.putAll(refModel.vendorExtensions);
                    // rewrite per model extensions by per property extensions
                    tmp.putAll(cp.vendorExtensions);
                    cp.vendorExtensions.putAll(tmp);
                }
            }
        }
        return objs;
    }

    protected void addOption(String key, String description, String defaultValue) {
        CliOption option = new CliOption(key, description);
        if (defaultValue != null)
            option.defaultValue(defaultValue);
        cliOptions.add(option);
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (additionalProperties.containsKey(DEFAULT_INCLUDE)) {
            defaultInclude = additionalProperties.get(DEFAULT_INCLUDE).toString();
        }

        additionalProperties.put("modelNamespaceDeclarations", modelPackage.split("\\."));
        additionalProperties.put("modelNamespace", modelPackage.replaceAll("\\.", "::"));
        additionalProperties.put("defaultInclude", defaultInclude);
    }

    /**
     * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
     * those terms here.  This logic is only called if a variable matches the reseved words
     *
     * @return the escaped term
     */
    @Override
    public String escapeReservedWord(String name) {
        return "_" + name;  // add an underscore to the name
    }

    /**
     * Location to write model files.  You can use the modelPackage() as defined when the class is
     * instantiated
     */
    public String modelFileFolder() {
        return outputFolder + "/" + modelPackage().replace('.', File.separatorChar);
    }

    public String modelTestFileFolder()
    {
        return outputFolder + "/" + modelPackage().replace('.', File.separatorChar) + "/Tests";
    }

    @Override
    public String toModelImport(String name) {
        if (importMapping.containsKey(name)) {
            return importMapping.get(name);
        } else {
            return "#include \"" + name + ".h\"";
        }
    }

    @Override
    public CodegenModel fromModel(String name, Model model, Map<String, Model> allDefinitions) {
        CodegenModel codegenModel = super.fromModel(name, model, allDefinitions);

        Set<String> oldImports = codegenModel.imports;
        codegenModel.imports = new HashSet<String>();
        for (String imp : oldImports) {
            String newImp = toModelImport(imp);
            if (!newImp.isEmpty()) {
                codegenModel.imports.add(newImp);
            }
        }

        return codegenModel;
    }

    @Override
    public String toVarName(String name) {
        // replace - with _ e.g. created-at => created_at
        name = name.replaceAll("-", "_"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // if it's all uppper case, do nothing
        if (name.matches("^[A-Z_]*$")) {
            return name;
        }

        // camelize the variable name
        // pet_id => PetId
        name = camelize(name);

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches("^\\d.*")) {
            name = escapeReservedWord(name);
        }

        return name;
    }

    /**
     * Optional - type declaration.  This is a String which is used by the templates to instantiate your
     * types.  There is typically special handling for different property types
     *
     * @return a string value used as the `dataType` field for model templates, `returnType` for api templates
     */
    @Override
    public String getTypeDeclaration(Property p) {
        String swaggerType = getSwaggerType(p);
        String ret;
        if (p instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty) p;
            Property inner = ap.getItems();
            ret = swaggerType + "<" + getTypeDeclaration(inner) + ">";
        } else if (p instanceof MapProperty) {
            MapProperty mp = (MapProperty) p;
            Property inner = mp.getAdditionalProperties();
            ret = swaggerType + "<FString, " + getTypeDeclaration(inner) + ">";
        } else {
            ret = super.getTypeDeclaration(p);
        }

        if (!p.getRequired())
        {
            ret = "TOptional<" + ret + ">";
        }
        return ret;
    }

    @Override
    public String toDefaultValue(Property p) {
        if (p instanceof StringProperty) {
            StringProperty dp = (StringProperty) p;
            if (dp.getDefault() != null) {
                String _default = dp.getDefault();
                if (dp.getEnum() == null) {
                    return "\"" + _default + "\"";
                } else {
                    // convert to enum var name later in postProcessModels
                    return _default;
                }
            }
        } else if (p instanceof DateProperty) {
            return "FDateTime(0)";
        } else if (p instanceof DateTimeProperty) {
            return "FDateTime(0)";
        } else if (p instanceof BooleanProperty) {
            BooleanProperty dp = (BooleanProperty) p;
            if (dp.getDefault() != null) {
                return dp.getDefault().toString();
            } else {
                return "false";
            }
        } else if (p instanceof DoubleProperty) {
            DoubleProperty dp = (DoubleProperty) p;
            if (dp.getDefault() != null) {
                return dp.getDefault().toString();
            } else {
                return "0.0";
            }
        } else if (p instanceof DecimalProperty) {
            return "0.0";
        } else if (p instanceof FloatProperty) {
            FloatProperty dp = (FloatProperty) p;
            if (dp.getDefault() != null) {
                return String.format("%1$sF", dp.getDefault());
            } else {
                return "0.0f";
            }
        } else if (p instanceof IntegerProperty) {
            IntegerProperty dp = (IntegerProperty) p;
            if (dp.getDefault() != null) {
                return dp.getDefault().toString();
            } else {
                return "0";
            }
        } else if (p instanceof BaseIntegerProperty) {
                return "0";
        } else if (p instanceof LongProperty) {
            LongProperty dp = (LongProperty) p;
            if (dp.getDefault() != null) {
                return dp.getDefault().toString();
            } else {
                return "0";
            }
        }
        return null;
    }

    /**
     * Optional - swagger type conversion.  This is used to map swagger types in a `Property` into
     * either language specific types via `typeMapping` or into complex models if there is not a mapping.
     *
     * @return a string value of the type or complex model for this property
     * @see io.swagger.models.properties.Property
     */
    @Override
    public String getSwaggerType(Property p) {
        String swaggerType = super.getSwaggerType(p);
        String type = null;
        if (typeMapping.containsKey(swaggerType.toLowerCase())) {
            type = typeMapping.get(swaggerType.toLowerCase());
            if (languageSpecificPrimitives.contains(type)) {
                return type;
            }
        } else {
            type = swaggerType;
        }
        return toModelName(type);
    }
}