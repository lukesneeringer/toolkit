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
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.ApiMethodDocView;
import com.google.api.codegen.viewmodel.CallableMethodDetailView;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.DynamicLangDefaultableParamView;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.ListMethodDetailView;
import com.google.api.codegen.viewmodel.MapParamDocView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.ParamDocView;
import com.google.api.codegen.viewmodel.PathTemplateCheckView;
import com.google.api.codegen.viewmodel.RequestObjectMethodDetailView;
import com.google.api.codegen.viewmodel.RequestObjectParamView;
import com.google.api.codegen.viewmodel.SimpleParamDocView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView.Builder;
import com.google.api.codegen.viewmodel.UnpagedListCallableMethodDetailView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** ApiMethodTransformer generates view objects from method definitions. */
public class ApiMethodTransformer {
  private final InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
  private final LongRunningTransformer lroTransformer = new LongRunningTransformer();

  public StaticLangApiMethodView generatePagedFlattenedMethod(MethodTransformerContext context) {
    return generatePagedFlattenedMethod(context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generatePagedFlattenedMethod(
      MethodTransformerContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterface(), context.getMethod()));
    setListMethodFields(context, Synchronicity.Sync, methodViewBuilder);
    setFlattenedMethodFields(context, additionalParams, Synchronicity.Sync, methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.PagedFlattenedMethod).build();
  }

  public StaticLangApiMethodView generatePagedFlattenedAsyncMethod(
      MethodTransformerContext context) {
    return generatePagedFlattenedAsyncMethod(context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generatePagedFlattenedAsyncMethod(
      MethodTransformerContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(
            context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getAsyncApiMethodExampleName(context.getInterface(), context.getMethod()));
    setListMethodFields(context, Synchronicity.Async, methodViewBuilder);
    setFlattenedMethodFields(context, additionalParams, Synchronicity.Async, methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.PagedFlattenedAsyncMethod).build();
  }

  public StaticLangApiMethodView generatePagedRequestObjectMethod(
      MethodTransformerContext context) {
    return generatePagedRequestObjectMethod(context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generatePagedRequestObjectMethod(
      MethodTransformerContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterface(), context.getMethod()));
    setListMethodFields(context, Synchronicity.Sync, methodViewBuilder);
    setRequestObjectMethodFields(
        context,
        namer.getPagedCallableMethodName(context.getMethod()),
        Synchronicity.Sync,
        additionalParams,
        methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.PagedRequestObjectMethod).build();
  }

  public StaticLangApiMethodView generatePagedRequestObjectAsyncMethod(
      MethodTransformerContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(
            context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getAsyncApiMethodExampleName(context.getInterface(), context.getMethod()));
    setListMethodFields(context, Synchronicity.Async, methodViewBuilder);
    setRequestObjectMethodFields(
        context,
        namer.getPagedCallableMethodName(context.getMethod()),
        Synchronicity.Async,
        additionalParams,
        methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.AsyncPagedRequestObjectMethod).build();
  }

  public StaticLangApiMethodView generatePagedCallableMethod(MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(namer.getPagedCallableMethodName(context.getMethod()));
    methodViewBuilder.exampleName(
        namer.getPagedCallableMethodExampleName(context.getInterface(), context.getMethod()));
    setListMethodFields(context, Synchronicity.Sync, methodViewBuilder);
    setCallableMethodFields(
        context, namer.getPagedCallableName(context.getMethod()), methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.PagedCallableMethod).build();
  }

  public StaticLangApiMethodView generateUnpagedListCallableMethod(
      MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(namer.getCallableMethodName(context.getMethod()));
    methodViewBuilder.exampleName(
        namer.getCallableMethodExampleName(context.getInterface(), context.getMethod()));
    setListMethodFields(context, Synchronicity.Sync, methodViewBuilder);
    setCallableMethodFields(context, namer.getCallableName(context.getMethod()), methodViewBuilder);

    String getResourceListCallName =
        namer.getFieldGetFunctionName(
            context.getFeatureConfig(),
            context.getMethodConfig().getPageStreaming().getResourcesFieldConfig());

    UnpagedListCallableMethodDetailView unpagedListCallableDetails =
        UnpagedListCallableMethodDetailView.newBuilder()
            .resourceListGetFunction(getResourceListCallName)
            .build();
    methodViewBuilder.unpagedListCallableMethod(unpagedListCallableDetails);

    methodViewBuilder.responseTypeName(
        context.getTypeTable().getAndSaveNicknameFor(context.getMethod().getOutputType()));

    return methodViewBuilder.type(ClientMethodType.UnpagedListCallableMethod).build();
  }

  public StaticLangApiMethodView generateFlattenedAsyncMethod(
      MethodTransformerContext context, ClientMethodType type) {
    return generateFlattenedAsyncMethod(context, Collections.<ParamWithSimpleDoc>emptyList(), type);
  }

  public StaticLangApiMethodView generateFlattenedAsyncMethod(
      MethodTransformerContext context,
      List<ParamWithSimpleDoc> additionalParams,
      ClientMethodType type) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(
            context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getCallableMethodExampleName(context.getInterface(), context.getMethod()));
    methodViewBuilder.callableName(namer.getCallableName(context.getMethod()));
    setFlattenedMethodFields(context, additionalParams, Synchronicity.Async, methodViewBuilder);
    setStaticLangAsyncReturnTypeName(context, methodViewBuilder);

    return methodViewBuilder.type(type).build();
  }

  public StaticLangApiMethodView generateFlattenedMethod(MethodTransformerContext context) {
    return generateFlattenedMethod(context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generateFlattenedMethod(
      MethodTransformerContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterface(), context.getMethod()));
    methodViewBuilder.callableName(namer.getCallableName(context.getMethod()));
    setFlattenedMethodFields(context, additionalParams, Synchronicity.Sync, methodViewBuilder);
    setStaticLangReturnTypeName(context, methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.FlattenedMethod).build();
  }

  public StaticLangApiMethodView generateRequestObjectMethod(MethodTransformerContext context) {
    return generateRequestObjectMethod(context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generateRequestObjectMethod(
      MethodTransformerContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterface(), context.getMethod()));
    setRequestObjectMethodFields(
        context,
        namer.getCallableMethodName(context.getMethod()),
        Synchronicity.Sync,
        additionalParams,
        methodViewBuilder);
    setStaticLangReturnTypeName(context, methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.RequestObjectMethod).build();
  }

  public StaticLangApiMethodView generateRequestObjectAsyncMethod(
      MethodTransformerContext context) {
    return generateRequestObjectAsyncMethod(context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generateRequestObjectAsyncMethod(
      MethodTransformerContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(
            context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getAsyncApiMethodExampleName(context.getInterface(), context.getMethod()));
    setRequestObjectMethodFields(
        context,
        namer.getCallableAsyncMethodName(context.getMethod()),
        Synchronicity.Async,
        additionalParams,
        methodViewBuilder);
    setStaticLangAsyncReturnTypeName(context, methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.AsyncRequestObjectMethod).build();
  }

  public StaticLangApiMethodView generateCallableMethod(MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(namer.getCallableMethodName(context.getMethod()));
    methodViewBuilder.exampleName(
        context
            .getNamer()
            .getCallableMethodExampleName(context.getInterface(), context.getMethod()));
    setCallableMethodFields(context, namer.getCallableName(context.getMethod()), methodViewBuilder);
    methodViewBuilder.responseTypeName(
        context.getTypeTable().getAndSaveNicknameFor(context.getMethod().getOutputType()));

    return methodViewBuilder.type(ClientMethodType.CallableMethod).build();
  }

  public StaticLangApiMethodView generateGrpcStreamingRequestObjectMethod(
      MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getGrpcStreamingApiMethodName(
            context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        context
            .getNamer()
            .getGrpcStreamingApiMethodExampleName(context.getInterface(), context.getMethod()));
    setRequestObjectMethodFields(
        context,
        namer.getCallableMethodName(context.getMethod()),
        Synchronicity.Sync,
        methodViewBuilder);
    setStaticLangGrpcStreamingReturnTypeName(context, methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.RequestObjectMethod).build();
  }

  public StaticLangApiMethodView generateOperationRequestObjectMethod(
      MethodTransformerContext context) {
    return generateOperationRequestObjectMethod(
        context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generateOperationRequestObjectMethod(
      MethodTransformerContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterface(), context.getMethod()));
    setRequestObjectMethodFields(
        context,
        namer.getCallableMethodName(context.getMethod()),
        Synchronicity.Sync,
        additionalParams,
        methodViewBuilder);
    methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));
    TypeRef returnType = context.getMethodConfig().getLongRunningConfig().getReturnType();
    methodViewBuilder.responseTypeName(context.getTypeTable().getAndSaveNicknameFor(returnType));

    return methodViewBuilder.type(ClientMethodType.OperationRequestObjectMethod).build();
  }

  public StaticLangApiMethodView generateOperationFlattenedMethod(
      MethodTransformerContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterface(), context.getMethod()));
    methodViewBuilder.callableName(namer.getCallableName(context.getMethod()));
    setFlattenedMethodFields(context, additionalParams, Synchronicity.Sync, methodViewBuilder);
    methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));
    TypeRef returnType = context.getMethodConfig().getLongRunningConfig().getReturnType();
    methodViewBuilder.responseTypeName(context.getTypeTable().getAndSaveNicknameFor(returnType));

    return methodViewBuilder.type(ClientMethodType.OperationFlattenedMethod).build();
  }

  public StaticLangApiMethodView generateAsyncOperationFlattenedMethod(
      MethodTransformerContext context) {
    return generateAsyncOperationFlattenedMethod(
        context,
        Collections.<ParamWithSimpleDoc>emptyList(),
        ClientMethodType.AsyncOperationFlattenedMethod,
        false);
  }

  public StaticLangApiMethodView generateAsyncOperationFlattenedMethod(
      MethodTransformerContext context,
      List<ParamWithSimpleDoc> additionalParams,
      ClientMethodType type,
      boolean requiresOperationMethod) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(
            context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getAsyncApiMethodExampleName(context.getInterface(), context.getMethod()));
    methodViewBuilder.callableName(namer.getCallableName(context.getMethod()));
    setFlattenedMethodFields(context, additionalParams, Synchronicity.Async, methodViewBuilder);
    if (requiresOperationMethod) {
      methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));
    }
    TypeRef returnType = context.getMethodConfig().getLongRunningConfig().getReturnType();
    methodViewBuilder.responseTypeName(context.getTypeTable().getAndSaveNicknameFor(returnType));

    return methodViewBuilder.type(type).build();
  }

  public StaticLangApiMethodView generateAsyncOperationRequestObjectMethod(
      MethodTransformerContext context) {
    return generateAsyncOperationRequestObjectMethod(
        context, Collections.<ParamWithSimpleDoc>emptyList(), false);
  }

  public StaticLangApiMethodView generateAsyncOperationRequestObjectMethod(
      MethodTransformerContext context,
      List<ParamWithSimpleDoc> additionalParams,
      boolean requiresOperationMethod) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(
            context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getAsyncApiMethodExampleName(context.getInterface(), context.getMethod()));
    setRequestObjectMethodFields(
        context,
        namer.getOperationCallableMethodName(context.getMethod()),
        Synchronicity.Async,
        additionalParams,
        methodViewBuilder);
    if (requiresOperationMethod) {
      methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));
    }
    TypeRef returnType = context.getMethodConfig().getLongRunningConfig().getReturnType();
    methodViewBuilder.responseTypeName(context.getTypeTable().getAndSaveNicknameFor(returnType));

    return methodViewBuilder.type(ClientMethodType.AsyncOperationRequestObjectMethod).build();
  }

  public StaticLangApiMethodView generateOperationCallableMethod(MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(namer.getOperationCallableMethodName(context.getMethod()));
    methodViewBuilder.exampleName(
        context
            .getNamer()
            .getOperationCallableMethodExampleName(context.getInterface(), context.getMethod()));
    setCallableMethodFields(
        context, namer.getOperationCallableName(context.getMethod()), methodViewBuilder);
    TypeRef returnType = context.getMethodConfig().getLongRunningConfig().getReturnType();
    methodViewBuilder.responseTypeName(context.getTypeTable().getAndSaveNicknameFor(returnType));

    return methodViewBuilder.type(ClientMethodType.OperationCallableMethod).build();
  }

  private void setCommonFields(
      MethodTransformerContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();

    String requestTypeName =
        context.getTypeTable().getAndSaveNicknameFor(context.getMethod().getInputType());
    methodViewBuilder.serviceRequestTypeName(requestTypeName);
    methodViewBuilder.serviceRequestTypeConstructor(namer.getTypeConstructor(requestTypeName));

    if (context.getMethodConfig().isGrpcStreaming()) {
      String returnTypeFullName = namer.getGrpcStreamingApiReturnTypeName(context.getMethod());
      String returnTypeNickname = context.getTypeTable().getAndSaveNicknameFor(returnTypeFullName);
      methodViewBuilder.serviceResponseTypeName(returnTypeNickname);

    } else {
      String responseTypeName =
          context.getTypeTable().getAndSaveNicknameFor(context.getMethod().getOutputType());
      methodViewBuilder.serviceResponseTypeName(responseTypeName);
    }

    methodViewBuilder.apiClassName(namer.getApiWrapperClassName(context.getInterface()));
    methodViewBuilder.apiVariableName(namer.getApiWrapperVariableName(context.getInterface()));
    methodViewBuilder.stubName(namer.getStubName(context.getTargetInterface()));
    methodViewBuilder.settingsGetterName(namer.getSettingsFunctionName(context.getMethod()));
    methodViewBuilder.callableName(context.getNamer().getCallableName(context.getMethod()));
    methodViewBuilder.modifyMethodName(namer.getModifyMethodName(context.getMethod()));
    methodViewBuilder.grpcStreamingType(context.getMethodConfig().getGrpcStreamingType());
    methodViewBuilder.visibility(
        namer.getVisiblityKeyword(context.getMethodConfig().getVisibility()));

    ServiceMessages messages = new ServiceMessages();
    if (context.getMethodConfig().isLongRunningOperation()) {
      methodViewBuilder.hasReturnValue(
          !messages.isEmptyType(context.getMethodConfig().getLongRunningConfig().getReturnType()));
    } else {
      methodViewBuilder.hasReturnValue(!messages.isEmptyType(context.getMethod().getOutputType()));
    }
  }

  private void setListMethodFields(
      MethodTransformerContext context,
      Synchronicity synchronicity,
      StaticLangApiMethodView.Builder methodViewBuilder) {
    ModelTypeTable typeTable = context.getTypeTable();
    SurfaceNamer namer = context.getNamer();
    PageStreamingConfig pageStreaming = context.getMethodConfig().getPageStreaming();
    String requestTypeName = typeTable.getAndSaveNicknameFor(context.getMethod().getInputType());
    String responseTypeName = typeTable.getAndSaveNicknameFor(context.getMethod().getOutputType());

    FieldConfig resourceFieldConfig = pageStreaming.getResourcesFieldConfig();
    Field resourceField = resourceFieldConfig.getField();

    String resourceTypeName;

    if (context.getFeatureConfig().useResourceNameFormatOption(resourceFieldConfig)) {
      resourceTypeName = namer.getAndSaveElementResourceTypeName(typeTable, resourceFieldConfig);
    } else {
      resourceTypeName = typeTable.getAndSaveNicknameForElementType(resourceField.getType());
    }

    String iterateMethodName =
        context
            .getNamer()
            .getPagedResponseIterateMethod(context.getFeatureConfig(), resourceFieldConfig);

    String resourceFieldName = context.getNamer().getFieldName(resourceField);
    String resourceFieldGetFunctionName =
        namer.getFieldGetFunctionName(context.getFeatureConfig(), resourceFieldConfig);

    methodViewBuilder.listMethod(
        ListMethodDetailView.newBuilder()
            .requestTypeName(requestTypeName)
            .responseTypeName(responseTypeName)
            .resourceTypeName(resourceTypeName)
            .iterateMethodName(iterateMethodName)
            .resourceFieldName(resourceFieldName)
            .resourcesFieldGetFunction(resourceFieldGetFunctionName)
            .build());

    switch (synchronicity) {
      case Sync:
        methodViewBuilder.responseTypeName(
            namer.getAndSavePagedResponseTypeName(
                context.getMethod(), context.getTypeTable(), resourceFieldConfig));
        break;
      case Async:
        methodViewBuilder.responseTypeName(
            namer.getAndSaveAsyncPagedResponseTypeName(
                context.getMethod(), context.getTypeTable(), resourceFieldConfig));
        break;
    }
  }

  private void setFlattenedMethodFields(
      MethodTransformerContext context,
      List<ParamWithSimpleDoc> additionalParams,
      Synchronicity synchronicity,
      StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    Iterable<FieldConfig> fieldConfigs =
        context.getFlatteningConfig().getFlattenedFieldConfigs().values();
    methodViewBuilder.initCode(
        initCodeTransformer.generateInitCode(
            context.cloneWithEmptyTypeTable(),
            createInitCodeContext(context, fieldConfigs, InitCodeOutputType.FieldList)));
    methodViewBuilder.doc(
        ApiMethodDocView.newBuilder()
            .mainDocLines(namer.getDocLines(context.getMethod(), context.getMethodConfig()))
            .paramDocs(getMethodParamDocs(context, fieldConfigs, additionalParams))
            .throwsDocLines(namer.getThrowsDocLines())
            .returnsDocLines(
                namer.getReturnDocLines(
                    context.getSurfaceTransformerContext(),
                    context.getMethodConfig(),
                    synchronicity))
            .build());

    List<RequestObjectParamView> params = new ArrayList<>();
    for (FieldConfig fieldConfig : fieldConfigs) {
      params.add(generateRequestObjectParam(context, fieldConfig));
    }
    methodViewBuilder.forwardingMethodParams(params);
    List<RequestObjectParamView> nonforwardingParams = new ArrayList<>(params);
    nonforwardingParams.addAll(ParamWithSimpleDoc.asRequestObjectParamViews(additionalParams));
    methodViewBuilder.methodParams(nonforwardingParams);
    methodViewBuilder.requestObjectParams(params);

    methodViewBuilder.pathTemplateChecks(generatePathTemplateChecks(context, fieldConfigs));
  }

  private void setRequestObjectMethodFields(
      MethodTransformerContext context,
      String callableMethodName,
      Synchronicity sync,
      StaticLangApiMethodView.Builder methodViewBuilder) {
    setRequestObjectMethodFields(
        context,
        callableMethodName,
        sync,
        Collections.<ParamWithSimpleDoc>emptyList(),
        methodViewBuilder);
  }

  private void setRequestObjectMethodFields(
      MethodTransformerContext context,
      String callableMethodName,
      Synchronicity sync,
      List<ParamWithSimpleDoc> additionalParams,
      StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    List<ParamDocView> paramDocs = new ArrayList<>();
    paramDocs.add(getRequestObjectParamDoc(context, context.getMethod().getInputType()));
    paramDocs.addAll(ParamWithSimpleDoc.asParamDocViews(additionalParams));
    methodViewBuilder.doc(
        ApiMethodDocView.newBuilder()
            .mainDocLines(namer.getDocLines(context.getMethod(), context.getMethodConfig()))
            .paramDocs(paramDocs)
            .throwsDocLines(namer.getThrowsDocLines())
            .returnsDocLines(
                namer.getReturnDocLines(
                    context.getSurfaceTransformerContext(), context.getMethodConfig(), sync))
            .build());
    methodViewBuilder.initCode(
        initCodeTransformer.generateInitCode(
            context.cloneWithEmptyTypeTable(),
            createInitCodeContext(
                context,
                context.getMethodConfig().getRequiredFieldConfigs(),
                InitCodeOutputType.SingleObject)));

    methodViewBuilder.methodParams(new ArrayList<RequestObjectParamView>());
    methodViewBuilder.requestObjectParams(new ArrayList<RequestObjectParamView>());
    methodViewBuilder.pathTemplateChecks(new ArrayList<PathTemplateCheckView>());

    RequestObjectMethodDetailView.Builder detailBuilder =
        RequestObjectMethodDetailView.newBuilder();
    if (context.getMethodConfig().hasRequestObjectMethod()) {
      detailBuilder.accessModifier(context.getNamer().getPublicAccessModifier());
    } else {
      detailBuilder.accessModifier(context.getNamer().getPrivateAccessModifier());
    }
    detailBuilder.callableMethodName(callableMethodName);
    methodViewBuilder.requestObjectMethod(detailBuilder.build());
  }

  private void setCallableMethodFields(
      MethodTransformerContext context, String callableName, Builder methodViewBuilder) {
    methodViewBuilder.doc(
        ApiMethodDocView.newBuilder()
            .mainDocLines(
                context.getNamer().getDocLines(context.getMethod(), context.getMethodConfig()))
            .paramDocs(new ArrayList<ParamDocView>())
            .throwsDocLines(new ArrayList<String>())
            .build());
    methodViewBuilder.initCode(
        initCodeTransformer.generateInitCode(
            context.cloneWithEmptyTypeTable(),
            createInitCodeContext(
                context,
                context.getMethodConfig().getRequiredFieldConfigs(),
                InitCodeOutputType.SingleObject)));

    methodViewBuilder.methodParams(new ArrayList<RequestObjectParamView>());
    methodViewBuilder.requestObjectParams(new ArrayList<RequestObjectParamView>());
    methodViewBuilder.pathTemplateChecks(new ArrayList<PathTemplateCheckView>());

    String genericAwareResponseTypeFullName =
        context.getNamer().getGenericAwareResponseTypeName(context.getMethod().getOutputType());
    String genericAwareResponseType =
        context.getTypeTable().getAndSaveNicknameFor(genericAwareResponseTypeFullName);
    methodViewBuilder.callableMethod(
        CallableMethodDetailView.newBuilder()
            .genericAwareResponseType(genericAwareResponseType)
            .callableName(callableName)
            .build());
  }

  private void setStaticLangAsyncReturnTypeName(
      MethodTransformerContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    String returnTypeFullName =
        namer.getStaticLangAsyncReturnTypeName(context.getMethod(), context.getMethodConfig());
    String returnTypeNickname = context.getTypeTable().getAndSaveNicknameFor(returnTypeFullName);
    methodViewBuilder.responseTypeName(returnTypeNickname);
  }

  private void setStaticLangReturnTypeName(
      MethodTransformerContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    String returnTypeFullName =
        namer.getStaticLangReturnTypeName(context.getMethod(), context.getMethodConfig());
    String returnTypeNickname = context.getTypeTable().getAndSaveNicknameFor(returnTypeFullName);
    methodViewBuilder.responseTypeName(returnTypeNickname);
  }

  private void setStaticLangGrpcStreamingReturnTypeName(
      MethodTransformerContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    // use the api return type name as the surface return type name
    String returnTypeFullName = namer.getGrpcStreamingApiReturnTypeName(context.getMethod());
    String returnTypeNickname = context.getTypeTable().getAndSaveNicknameFor(returnTypeFullName);
    methodViewBuilder.responseTypeName(returnTypeNickname);
  }

  private List<PathTemplateCheckView> generatePathTemplateChecks(
      MethodTransformerContext context, Iterable<FieldConfig> fieldConfigs) {
    List<PathTemplateCheckView> pathTemplateChecks = new ArrayList<>();
    if (!context.getFeatureConfig().enableStringFormatFunctions()) {
      return pathTemplateChecks;
    }
    for (FieldConfig fieldConfig : fieldConfigs) {
      if (!fieldConfig.useValidation()) {
        // Don't generate a path template check if fieldConfig is not configured to use validation.
        continue;
      }
      Field field = fieldConfig.getField();
      ImmutableMap<String, String> fieldNamePatterns =
          context.getMethodConfig().getFieldNamePatterns();
      String entityName = fieldNamePatterns.get(field.getSimpleName());
      if (entityName != null) {
        SingleResourceNameConfig resourceNameConfig =
            context.getSingleResourceNameConfig(entityName);
        if (resourceNameConfig == null) {
          String methodName = context.getMethod().getSimpleName();
          throw new IllegalStateException(
              "No collection config with id '"
                  + entityName
                  + "' required by configuration for method '"
                  + methodName
                  + "'");
        }
        PathTemplateCheckView.Builder check = PathTemplateCheckView.newBuilder();
        check.pathTemplateName(
            context.getNamer().getPathTemplateName(context.getInterface(), resourceNameConfig));
        check.paramName(context.getNamer().getVariableName(field));
        check.allowEmptyString(shouldAllowEmpty(context, field));
        check.validationMessageContext(
            context
                .getNamer()
                .getApiMethodName(context.getMethod(), context.getMethodConfig().getVisibility()));
        pathTemplateChecks.add(check.build());
      }
    }
    return pathTemplateChecks;
  }

  private boolean shouldAllowEmpty(MethodTransformerContext context, Field field) {
    for (Field requiredField : context.getMethodConfig().getRequiredFields()) {
      if (requiredField.equals(field)) {
        return false;
      }
    }
    return true;
  }

  public OptionalArrayMethodView generateDynamicLangApiMethod(MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    OptionalArrayMethodView.Builder apiMethod = OptionalArrayMethodView.newBuilder();

    if (context.getMethodConfig().isPageStreaming()) {
      apiMethod.type(ClientMethodType.PagedOptionalArrayMethod);
    } else {
      apiMethod.type(ClientMethodType.OptionalArrayMethod);
    }
    apiMethod.apiClassName(namer.getApiWrapperClassName(context.getInterface()));
    apiMethod.apiVariableName(namer.getApiWrapperVariableName(context.getInterface()));
    apiMethod.apiModuleName(namer.getApiWrapperModuleName());
    InitCodeOutputType initCodeOutputType =
        context.getMethod().getRequestStreaming()
            ? InitCodeOutputType.SingleObject
            : InitCodeOutputType.FieldList;
    InitCodeView initCode =
        initCodeTransformer.generateInitCode(
            context.cloneWithEmptyTypeTable(),
            createInitCodeContext(
                context, context.getMethodConfig().getRequiredFieldConfigs(), initCodeOutputType));
    apiMethod.initCode(initCode);

    apiMethod.doc(generateOptionalArrayMethodDoc(context));

    apiMethod.name(
        namer.getApiMethodName(context.getMethod(), context.getMethodConfig().getVisibility()));
    apiMethod.requestTypeName(
        context.getTypeTable().getAndSaveNicknameFor(context.getMethod().getInputType()));
    apiMethod.hasRequestParameters(initCode.lines().size() > 0);
    apiMethod.hasReturnValue(!ServiceMessages.s_isEmptyType(context.getMethod().getOutputType()));
    apiMethod.key(namer.getMethodKey(context.getMethod()));
    apiMethod.grpcMethodName(namer.getGrpcMethodName(context.getMethod()));
    apiMethod.stubName(namer.getStubName(context.getTargetInterface()));

    apiMethod.methodParams(generateOptionalArrayMethodParams(context));

    apiMethod.requiredRequestObjectParams(
        generateRequestObjectParams(context, context.getMethodConfig().getRequiredFieldConfigs()));
    apiMethod.optionalRequestObjectParams(
        generateRequestObjectParams(context, context.getMethodConfig().getOptionalFieldConfigs()));
    Iterable<FieldConfig> filteredFieldConfigs =
        removePageTokenFieldConfig(context, context.getMethodConfig().getOptionalFieldConfigs());
    apiMethod.optionalRequestObjectParamsNoPageToken(
        generateRequestObjectParams(context, filteredFieldConfigs));
    apiMethod.grpcStreamingType(context.getMethodConfig().getGrpcStreamingType());

    apiMethod.isLongRunningOperation(context.getMethodConfig().isLongRunningOperation());
    apiMethod.longRunningView(
        context.getMethodConfig().isLongRunningOperation()
            ? lroTransformer.generateDetailView(context)
            : null);

    return apiMethod.build();
  }

  private ApiMethodDocView generateOptionalArrayMethodDoc(MethodTransformerContext context) {
    ApiMethodDocView.Builder docBuilder = ApiMethodDocView.newBuilder();

    docBuilder.mainDocLines(
        context.getNamer().getDocLines(context.getMethod(), context.getMethodConfig()));
    List<ParamDocView> paramDocs =
        getMethodParamDocs(
            context,
            context.getMethodConfig().getRequiredFieldConfigs(),
            Collections.<ParamWithSimpleDoc>emptyList());
    paramDocs.add(
        getOptionalArrayParamDoc(context, context.getMethodConfig().getOptionalFieldConfigs()));
    docBuilder.paramDocs(paramDocs);
    docBuilder.returnTypeName(
        context
            .getNamer()
            .getDynamicLangReturnTypeName(context.getMethod(), context.getMethodConfig()));
    docBuilder.throwsDocLines(new ArrayList<String>());

    return docBuilder.build();
  }

  private List<DynamicLangDefaultableParamView> generateOptionalArrayMethodParams(
      MethodTransformerContext context) {
    List<DynamicLangDefaultableParamView> methodParams =
        generateDefaultableParams(context, context.getMethodConfig().getRequiredFields());

    // TODO create a map TypeRef here instead of an array
    // (not done yet because array is sufficient for PHP, and maps are more complex to construct)
    TypeRef arrayType = TypeRef.fromPrimitiveName("string").makeRepeated();

    DynamicLangDefaultableParamView.Builder optionalArgs =
        DynamicLangDefaultableParamView.newBuilder();
    optionalArgs.name(context.getNamer().localVarName(Name.from("optional", "args")));
    optionalArgs.defaultValue(context.getTypeTable().getZeroValueAndSaveNicknameFor(arrayType));
    methodParams.add(optionalArgs.build());

    return methodParams;
  }

  private List<DynamicLangDefaultableParamView> generateDefaultableParams(
      MethodTransformerContext context, Iterable<Field> fields) {
    List<DynamicLangDefaultableParamView> methodParams = new ArrayList<>();
    for (Field field : context.getMethodConfig().getRequiredFields()) {
      DynamicLangDefaultableParamView param =
          DynamicLangDefaultableParamView.newBuilder()
              .name(context.getNamer().getVariableName(field))
              .defaultValue("")
              .build();
      methodParams.add(param);
    }
    return methodParams;
  }

  private List<RequestObjectParamView> generateRequestObjectParams(
      MethodTransformerContext context, Iterable<FieldConfig> fieldConfigs) {
    List<RequestObjectParamView> params = new ArrayList<>();
    for (FieldConfig fieldConfig : fieldConfigs) {
      params.add(generateRequestObjectParam(context, fieldConfig));
    }
    return params;
  }

  private Iterable<FieldConfig> removePageTokenFieldConfig(
      MethodTransformerContext context, Iterable<FieldConfig> fieldConfigs) {
    MethodConfig methodConfig = context.getMethodConfig();
    if (methodConfig == null || !methodConfig.isPageStreaming()) {
      return fieldConfigs;
    }
    final Field requestTokenField = methodConfig.getPageStreaming().getRequestTokenField();
    return Iterables.filter(
        fieldConfigs,
        new Predicate<FieldConfig>() {
          @Override
          public boolean apply(FieldConfig fieldConfig) {
            return !fieldConfig.getField().equals(requestTokenField);
          }
        });
  }

  private RequestObjectParamView generateRequestObjectParam(
      MethodTransformerContext context, FieldConfig fieldConfig) {
    SurfaceNamer namer = context.getNamer();
    FeatureConfig featureConfig = context.getFeatureConfig();
    ModelTypeTable typeTable = context.getTypeTable();
    Field field = fieldConfig.getField();

    String typeName =
        namer.getNotImplementedString("ApiMethodTransformer.generateRequestObjectParam - typeName");
    String elementTypeName =
        namer.getNotImplementedString(
            "ApiMethodTransformer.generateRequestObjectParam - elementTypeName");

    if (context.getFeatureConfig().useResourceNameFormatOption(fieldConfig)) {
      if (namer.shouldImportRequestObjectParamType(field)) {
        typeName = namer.getAndSaveResourceTypeName(typeTable, fieldConfig);
      }
      if (namer.shouldImportRequestObjectParamElementType(field)) {
        // Use makeOptional to remove repeated property from type
        elementTypeName = namer.getAndSaveElementResourceTypeName(typeTable, fieldConfig);
      }
    } else {
      if (namer.shouldImportRequestObjectParamType(field)) {
        typeName = typeTable.getAndSaveNicknameFor(field.getType());
      }
      if (namer.shouldImportRequestObjectParamElementType(field)) {
        elementTypeName = typeTable.getAndSaveNicknameForElementType(field.getType());
      }
    }

    String setCallName = namer.getFieldSetFunctionName(featureConfig, fieldConfig);
    String addCallName = namer.getFieldAddFunctionName(field);
    String transformParamFunctionName = null;
    if (context.getFeatureConfig().useResourceNameFormatOption(fieldConfig)
        && fieldConfig.hasDifferentMessageResourceNameConfig()) {
      transformParamFunctionName = namer.getResourceOneofCreateMethod(typeTable, fieldConfig);
    }

    RequestObjectParamView.Builder param = RequestObjectParamView.newBuilder();
    param.name(namer.getVariableName(field));
    param.nameAsMethodName(namer.getFieldGetFunctionName(featureConfig, fieldConfig));
    param.typeName(typeName);
    param.elementTypeName(elementTypeName);
    param.setCallName(setCallName);
    param.addCallName(addCallName);
    param.transformParamFunctionName(transformParamFunctionName);
    param.isMap(field.getType().isMap());
    param.isArray(!field.getType().isMap() && field.getType().isRepeated());
    return param.build();
  }

  private List<ParamDocView> getMethodParamDocs(
      MethodTransformerContext context,
      Iterable<FieldConfig> fieldConfigs,
      List<ParamWithSimpleDoc> additionalParamDocs) {
    List<ParamDocView> allDocs = new ArrayList<>();
    for (FieldConfig fieldConfig : fieldConfigs) {
      Field field = fieldConfig.getField();
      SimpleParamDocView.Builder paramDoc = SimpleParamDocView.newBuilder();
      paramDoc.paramName(context.getNamer().getVariableName(field));
      paramDoc.typeName(context.getTypeTable().getAndSaveNicknameFor(field.getType()));

      List<String> docLines = null;
      MethodConfig methodConfig = context.getMethodConfig();
      if (methodConfig.isPageStreaming()
          && methodConfig.getPageStreaming().hasPageSizeField()
          && field.equals(methodConfig.getPageStreaming().getPageSizeField())) {
        docLines =
            Arrays.asList(
                new String[] {
                  "The maximum number of resources contained in the underlying API",
                  "response. The API may return fewer values in a page, even if",
                  "there are additional values to be retrieved."
                });
      } else if (methodConfig.isPageStreaming()
          && field.equals(methodConfig.getPageStreaming().getRequestTokenField())) {
        docLines =
            Arrays.asList(
                new String[] {
                  "A page token is used to specify a page of values to be returned.",
                  "If no page token is specified (the default), the first page",
                  "of values will be returned. Any page token used here must have",
                  "been generated by a previous call to the API."
                });
      } else {
        docLines = context.getNamer().getDocLines(field);
      }

      paramDoc.lines(docLines);

      allDocs.add(paramDoc.build());
    }
    allDocs.addAll(ParamWithSimpleDoc.asParamDocViews(additionalParamDocs));
    return allDocs;
  }

  public SimpleParamDocView getRequestObjectParamDoc(
      MethodTransformerContext context, TypeRef typeRef) {
    return SimpleParamDocView.newBuilder()
        .paramName("request")
        .typeName(context.getTypeTable().getAndSaveNicknameFor(typeRef))
        .lines(
            Arrays.<String>asList(
                "The request object containing all of the parameters for the API call."))
        .build();
  }

  private ParamDocView getOptionalArrayParamDoc(
      MethodTransformerContext context, Iterable<FieldConfig> fieldConfigs) {
    MapParamDocView.Builder paramDoc = MapParamDocView.newBuilder();

    Name optionalArgsName = Name.from("optional", "args");

    paramDoc.paramName(context.getNamer().localVarName(optionalArgsName));
    paramDoc.typeName(context.getNamer().getOptionalArrayTypeName());

    List<String> docLines = Arrays.asList("Optional.");

    paramDoc.firstLine(docLines.get(0));
    paramDoc.remainingLines(docLines.subList(1, docLines.size()));

    paramDoc.arrayKeyDocs(
        ImmutableList.<ParamDocView>builder()
            .addAll(
                getMethodParamDocs(
                    context, fieldConfigs, Collections.<ParamWithSimpleDoc>emptyList()))
            .addAll(getCallSettingsParamDocList(context))
            .build());

    return paramDoc.build();
  }

  private List<ParamDocView> getCallSettingsParamDocList(MethodTransformerContext context) {
    List<ParamDocView> arrayKeyDocs = new ArrayList<>();
    SimpleParamDocView.Builder retrySettingsDoc = SimpleParamDocView.newBuilder();
    retrySettingsDoc.typeName(context.getNamer().getRetrySettingsTypeName());

    Name retrySettingsName = Name.from("retry", "settings");
    Name timeoutMillisName = Name.from("timeout", "millis");

    retrySettingsDoc.paramName(context.getNamer().localVarName(retrySettingsName));
    // TODO figure out a reliable way to line-wrap comments across all languages
    // instead of encoding it in the transformer
    String retrySettingsDocText =
        String.format(
            "Retry settings to use for this call. If present, then\n%s is ignored.",
            context.getNamer().varReference(timeoutMillisName));
    List<String> retrySettingsDocLines = context.getNamer().getDocLines(retrySettingsDocText);
    retrySettingsDoc.lines(retrySettingsDocLines);
    arrayKeyDocs.add(retrySettingsDoc.build());

    SimpleParamDocView.Builder timeoutDoc = SimpleParamDocView.newBuilder();
    timeoutDoc.typeName(context.getTypeTable().getAndSaveNicknameFor(TypeRef.of(Type.TYPE_INT32)));
    timeoutDoc.paramName(context.getNamer().localVarName(timeoutMillisName));
    // TODO figure out a reliable way to line-wrap comments across all languages
    // instead of encoding it in the transformer
    String timeoutMillisDocText =
        String.format(
            "Timeout to use for this call. Only used if %s\nis not set.",
            context.getNamer().varReference(retrySettingsName));
    List<String> timeoutMillisDocLines = context.getNamer().getDocLines(timeoutMillisDocText);
    timeoutDoc.lines(timeoutMillisDocLines);
    arrayKeyDocs.add(timeoutDoc.build());

    return arrayKeyDocs;
  }

  private InitCodeContext createInitCodeContext(
      MethodTransformerContext context,
      Iterable<FieldConfig> fieldConfigs,
      InitCodeOutputType initCodeOutputType) {
    return InitCodeContext.newBuilder()
        .initObjectType(context.getMethod().getInputType())
        .suggestedName(Name.from("request"))
        .initFieldConfigStrings(context.getMethodConfig().getSampleCodeInitFields())
        .initValueConfigMap(InitCodeTransformer.createCollectionMap(context))
        .initFields(FieldConfig.toFieldIterable(fieldConfigs))
        .outputType(initCodeOutputType)
        .fieldConfigMap(FieldConfig.toFieldConfigMap(fieldConfigs))
        .build();
  }
}
