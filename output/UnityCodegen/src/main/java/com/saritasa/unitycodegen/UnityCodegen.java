package com.saritasa.unitycodegen;

import io.swagger.codegen.v3.*;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenProperty;
import io.swagger.codegen.v3.CodegenType;
import io.swagger.codegen.v3.generators.DefaultCodegenConfig;

import io.swagger.codegen.v3.generators.util.OpenAPIUtil;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class UnityCodegen extends DefaultCodegenConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCodegenConfig.class);

    public static final String CLIENT_PACKAGE = "clientPackage";

    protected String packageName = "IO.Swagger";
    protected String packageVersion = "1.0.0";
    protected String clientPackage = "IO.Swagger.Client";

    private HashSet<String> nullableTypes = new HashSet<String>();

    public UnityCodegen() {
        super();

        // clear import mapping (from default generator) as C# (2.0) does not use it
        // at the moment
        importMapping.clear();

        supportsInheritance = true;
        supportsMixins = true;

        outputFolder = "generated-code" + File.separator + "UnityCodegen";
        // apiTemplateFiles.put("api.mustache", ".cs");
        embeddedTemplateDir = templateDir = "UnityCodegen";
        apiPackage = "IO.Swagger.Api";
        modelPackage = "IO.Swagger.Model";

        setReservedWordsLowerCase(
                Arrays.asList(
                    // local variable names in API methods (endpoints)
                    "path", "queryParams", "headerParams", "formParams", "fileParams", "postBody",
                    "authSettings", "response", "StatusCode",
                    // C# reserved word
                    "abstract", "as", "base", "bool", "break", "byte", "case", "catch", "char", "checked", "class", "const", "continue", "decimal", "default", "delegate", "do", "double", "else", "enum", "event", "explicit", "extern", "false", "finally", "fixed", "float", "for", "foreach", "goto", "if", "implicit", "in", "int", "interface", "internal", "is", "lock", "long", "namespace", "new", "null", "object", "operator", "out", "override", "params", "private", "protected", "public", "readonly", "ref", "return", "sbyte", "sealed", "short", "sizeof", "stackalloc", "static", "string", "struct", "switch", "this", "throw", "true", "try", "typeof", "uint", "ulong", "unchecked", "unsafe", "ushort", "using", "virtual", "void", "volatile", "while")
        );

        languageSpecificPrimitives = new HashSet<String>(
                Arrays.asList(
                        "String",
                        "string",
                        "bool",
                        "double",
                        "int",
                        "long",
                        "float",
                        "byte[]",
                        "List",
                        "Dictionary",
                        "DateTime",
                        "String",
                        "Boolean",
                        "Double",
                        "Integer",
                        "Long",
                        "Float",
                        "Guid",
                        "System.IO.Stream", // not really a primitive, we include it to avoid model import
                        "Object")
        );
        instantiationTypes.put("array", "List");
        instantiationTypes.put("map", "Dictionary");

        typeMapping = new HashMap<>();
        typeMapping.put("string", "string");
        typeMapping.put("boolean", "bool");
        typeMapping.put("integer", "int");
        typeMapping.put("float", "float");
        typeMapping.put("long", "long");
        typeMapping.put("double", "double");
        typeMapping.put("number", "double");
        typeMapping.put("BigDecimal", "double");
        typeMapping.put("datetime", "DateTime");
        typeMapping.put("date", "DateTime");
        typeMapping.put("file", "System.IO.Stream");
        typeMapping.put("array", "List");
        typeMapping.put("list", "List");
        typeMapping.put("map", "Dictionary");
        typeMapping.put("object", "Object");
        typeMapping.put("uuid", "Guid");

        nullableTypes.addAll(
                Arrays.asList(
                        "bool",
                        "int",
                        "float",
                        "long",
                        "double",
                        "DateTime",
                        "Guid")
        );

        cliOptions.clear();
        cliOptions.add(new CliOption(CodegenConstants.PACKAGE_NAME, "C# package name (convention: Camel.Case).")
                .defaultValue("IO.Swagger"));
        cliOptions.add(new CliOption(CodegenConstants.PACKAGE_VERSION, "C# package version.").defaultValue("1.0.0"));
        cliOptions.add(new CliOption(CLIENT_PACKAGE, "C# client package name (convention: Camel.Case).")
                .defaultValue("IO.Swagger.Client"));
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (additionalProperties.containsKey(CodegenConstants.PACKAGE_VERSION)) {
            setPackageVersion((String) additionalProperties.get(CodegenConstants.PACKAGE_VERSION));
        } else {
            additionalProperties.put(CodegenConstants.PACKAGE_VERSION, packageVersion);
        }

        if (additionalProperties.containsKey(CodegenConstants.PACKAGE_NAME)) {
            setPackageName((String) additionalProperties.get(CodegenConstants.PACKAGE_NAME));
            modelPackage = packageName + ".Model";
        } else {
            additionalProperties.put(CodegenConstants.PACKAGE_NAME, packageName);
        }

        if (additionalProperties.containsKey(CLIENT_PACKAGE)) {
            this.setClientPackage((String) additionalProperties.get(CLIENT_PACKAGE));
        } else {
            additionalProperties.put(CLIENT_PACKAGE, clientPackage);
        }

        modelTemplateFiles.put("model.mustache", ".cs");
        modelTestTemplateFiles.put("model-test.mustache", ".cs");
    }

    public void setClientPackage(String clientPackage) {
        this.clientPackage = clientPackage;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    @Override
    public String getName() {
        return "UnityCodegen";
    }

    @Override
    public String getHelp() {
        return "Generates Unity client library.";
    }

    @Override
    public String escapeReservedWord(String name) {           
        if(this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name);
        }
        return "_" + name;
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

    @Override
    public String toParamName(String name) {
        // replace - with _ e.g. created-at => created_at
        name = name.replaceAll("-", "_");

        // if it's all uppper case, do nothing
        if (name.matches("^[A-Z_]*$")) {
            return name;
        }

        // camelize(lower) the variable name
        // pet_id => petId
        name = camelize(name, true);

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches("^\\d.*")) {
            name = escapeReservedWord(name);
        }

        return name;
    }

    @Override
    public String toModelName(String name) {
        if (!StringUtils.isEmpty(modelNamePrefix)) {
            name = modelNamePrefix + "_" + name;
        }

        if (!StringUtils.isEmpty(modelNameSuffix)) {
            name = name + "_" + modelNameSuffix;
        }

        name = sanitizeName(name);

        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(name)) {
            LOGGER.warn(name + " (reserved word) cannot be used as model name. Renamed to " + camelize("model_" + name));
            name = "model_" + name; // e.g. return => ModelReturn (after camelize)
        }

        // model name starts with number
        if (name.matches("^\\d.*")) {
            LOGGER.warn(name + " (model name starts with number) cannot be used as model name. Renamed to " + camelize("model_" + name));
            name = "model_" + name; // e.g. 200Response => Model200Response (after camelize)
        }

        // camelize the model name
        // phone_number => PhoneNumber
        return camelize(name);
    }

    @Override
    public String getDefaultTemplateDir() {
        return templateDir;
    }

    @Override
    public String toModelFilename(String name) {
        // should be the same as the model name
        return toModelName(name);
    }

    @Override
    public CodegenProperty fromProperty(String name, Schema propertySchema) {
        CodegenProperty ret = super.fromProperty(name, propertySchema);
        if (StringUtils.isNotBlank(propertySchema.get$ref())) {
            Schema refSchema = OpenAPIUtil.getSchemaFromRefSchema(propertySchema, openAPI);

            if (refSchema != null && isObjectSchema(refSchema)) {
                if (refSchema.getNullable() != null) {
                    ret.nullable = refSchema.getNullable();
                }
            }
        }
        return ret;
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property){
        List<CodegenProperty> requiredProps = model.getRequiredVars();
        if (!requiredProps.contains(property) && nullableTypes.contains(property.datatype)) {
            property.datatype = getNullableTypeFor(property.datatype);
        }
    }

    @Override
    public String getTypeDeclaration(Schema p) {
        String swaggerType = getSchemaType(p);
        if (p instanceof ArraySchema) {
            ArraySchema ap = (ArraySchema) p;
            Schema inner = ap.getItems();
            return swaggerType + "<" + getTypeDeclaration(inner) + ">";
        } else if (p instanceof MapSchema) {
            MapSchema mp = (MapSchema) p;
            Object inner = mp.getAdditionalProperties();
            if (inner != null && !(inner instanceof Boolean)) {
                return swaggerType + "<String, Boolean>";
            } else if (inner != null && !(inner instanceof Schema)){
                return swaggerType + "<String, " + getTypeDeclaration((Schema)inner) + ">";
            }
        }
        return super.getTypeDeclaration(p);
    }

    private String getNullableTypeFor(String swaggerType) {
        return swaggerType + "?";
    }

    @Override
    public String getSchemaType(Schema p) {
        String swaggerType = super.getSchemaType(p);
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


    @Override
    public String escapeQuotationMark(String input) {
        // remove " to avoid code injection
        return input.replace("\"", "");
    }

    @Override
    public String modelTestFileFolder() {
        return outputFolder + File.separator + "API.Models.Test".replace('.', File.separatorChar);
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }
}