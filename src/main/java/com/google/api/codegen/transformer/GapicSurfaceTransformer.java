/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.transformer;

import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.LongRunningConfig;
import com.google.api.codegen.config.ServiceConfig;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.gapic.CommonGapicCodePathMapper;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.DynamicLangXApiView;
import com.google.api.codegen.viewmodel.LongRunningOperationDetailView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.TypeRef;
import java.util.ArrayList;
import java.util.List;

/** A base class to convert a Model representing an API into a client API for a given language. */
public abstract class GapicSurfaceTransformer implements ModelToViewTransformer {
  protected ApiMethodTransformer apiMethodTransformer = new ApiMethodTransformer();
  protected GrpcStubTransformer grpcStubTransformer = new GrpcStubTransformer();
  protected PageStreamingTransformer pageStreamingTransformer = new PageStreamingTransformer();
  protected PathTemplateTransformer pathTemplateTransformer = new PathTemplateTransformer();
  protected ServiceTransformer serviceTransformer = new ServiceTransformer();

  // These should be overridden on subclasses.
  protected GapicCodePathMapper pathMapper;
  protected ImportTypeTransformer importTypeTransformer;

  /**
   * The `transform` method is the primary entry point for a surface transformer. It should: 1.
   * Instantiate the appropriate namer and type table classes. 2. Iterate over the services, create
   * the appropriate context for each, and call transformContext` with the appropriate template.
   */
  public abstract List<ViewModel> transform(Model model, ApiConfig apiConfig);

  /**
   * The `getPathMapper` method should return a path mapper. Not every language needs a special path
   * mapper, but some do, and should override this method.
   */
  protected GapicCodePathMapper getPathMapper() {
    return CommonGapicCodePathMapper.defaultInstance();
  }

  /** Retrun the input type transformer for the language. */
  protected ImportTypeTransformer getImportTypeTransformer() {
    return new StandardImportTypeTransformer();
  }

  /** Convert a context to a full ViewModel. */
  public ViewModel transformContext(SurfaceTransformerContext context, String templateFileName) {
    ServiceConfig serviceConfig = new ServiceConfig();
    SurfaceNamer namer = context.getNamer();

    String outputPath =
        getPathMapper().getOutputPath(context.getInterface(), context.getApiConfig());
    String filename = namer.getApiWrapperClassName(context.getInterface());

    addApiImports(context);

    List<ApiMethodView> methods = generateApiMethods(context);

    return DynamicLangXApiView.newBuilder()
        .doc(serviceTransformer.generateServiceDoc(context, methods.get(0)))
        .templateFileName(templateFileName)
        .protoFilename(context.getInterface().getFile().getSimpleName())
        .packageName(context.getApiConfig().getPackageName())
        .name(filename)
        .serviceAddress(serviceConfig.getServiceAddress(context.getInterface()))
        .servicePort(serviceConfig.getServicePort())
        .serviceTitle(serviceConfig.getTitle(context.getInterface()))
        .authScopes(serviceConfig.getAuthScopes(context.getInterface()))
        .pathTemplates(pathTemplateTransformer.generatePathTemplates(context))
        .formatResourceFunctions(pathTemplateTransformer.generateFormatResourceFunctions(context))
        .parseResourceFunctions(pathTemplateTransformer.generateParseResourceFunctions(context))
        .pathTemplateGetterFunctions(
            pathTemplateTransformer.generatePathTemplateGetterFunctions(context))
        .pageStreamingDescriptors(pageStreamingTransformer.generateDescriptors(context))
        .longRunningDescriptors(createLongRunningDescriptors(context))
        .hasLongRunningOperations(context.getInterfaceConfig().hasLongRunningOperations())
        .methodKeys(generateMethodKeys(context))
        .clientConfigPath(namer.getClientConfigPath(context.getInterface()))
        .interfaceKey(context.getInterface().getFullName())
        .grpcClientTypeName(
            namer.getAndSaveNicknameForGrpcClientTypeName(
                context.getTypeTable(), context.getInterface()))
        .apiMethods(methods)
        .stubs(grpcStubTransformer.generateGrpcStubs(context))
        .hasDefaultServiceAddress(context.getInterfaceConfig().hasDefaultServiceAddress())
        .hasDefaultServiceScopes(context.getInterfaceConfig().hasDefaultServiceScopes())

        // This must be done as the last step to catch all imports.
        .imports(getImportTypeTransformer().generateImports(context.getTypeTable().getImports()))
        .outputPath(outputPath + "/" + filename + "." + namer.extension())
        .build();
  }

  protected List<LongRunningOperationDetailView> createLongRunningDescriptors(
      SurfaceTransformerContext context) {
    List<LongRunningOperationDetailView> result = new ArrayList<>();

    for (Method method : context.getLongRunningMethods()) {
      MethodTransformerContext methodContext = context.asDynamicMethodContext(method);
      LongRunningConfig lroConfig = methodContext.getMethodConfig().getLongRunningConfig();
      TypeRef returnType = lroConfig.getReturnType();
      TypeRef metadataType = lroConfig.getMetadataType();
      result.add(
          LongRunningOperationDetailView.newBuilder()
              .methodName(context.getNamer().getApiMethodName(method, VisibilityConfig.PUBLIC))
              .constructorName("")
              .clientReturnTypeName("")
              .operationPayloadTypeName(context.getTypeTable().getFullNameFor(returnType))
              .isEmptyOperation(ServiceMessages.s_isEmptyType(lroConfig.getReturnType()))
              .metadataTypeName(context.getTypeTable().getFullNameFor(metadataType))
              .implementsCancel(true)
              .implementsDelete(true)
              .build());
    }

    return result;
  }

  /**
   * Return a list of API methods that are supported in this permutation of target API and
   * implementation language.
   */
  private List<ApiMethodView> generateApiMethods(SurfaceTransformerContext context) {
    List<ApiMethodView> apiMethods = new ArrayList<>(context.getInterface().getMethods().size());

    for (Method method : context.getSupportedMethods()) {
      apiMethods.add(
          apiMethodTransformer.generateDynamicLangApiMethod(
              context.asDynamicMethodContext(method)));
    }

    return apiMethods;
  }

  /**
   * Return a list of method keys. A method key is the name of the method, formatted in the
   * idiomatic style of the implementation language (e.g. snake case for Python, camel case for
   * Java, etc.).
   */
  protected List<String> generateMethodKeys(SurfaceTransformerContext context) {
    List<String> methodKeys = new ArrayList<>(context.getInterface().getMethods().size());

    // Iterate over each method and generate the language-idiomatic
    // method names.
    for (Method method : context.getSupportedMethods()) {
      methodKeys.add(context.getNamer().getMethodKey(method));
    }

    return methodKeys;
  }

  /**
   * Add any API imports necessary, in the appropriate format for the language. This only needs to
   * add "general" API imports that cannot be determined from the context data.
   */
  protected abstract void addApiImports(SurfaceTransformerContext context);
}
