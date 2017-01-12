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
package com.google.api.codegen.transformer.php;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.php.PhpGapicCodePathMapper;
import com.google.api.codegen.transformer.GapicSurfaceTransformer;
import com.google.api.codegen.transformer.ImportTypeTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.php.PhpTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The ModelToViewTransformer to transform a Model into the standard GAPIC surface in PHP. */
public class PhpGapicSurfaceTransformer extends GapicSurfaceTransformer {
  private static final String XAPI_TEMPLATE_FILENAME = "php/main.snip";

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(XAPI_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      ModelTypeTable modelTypeTable =
          new ModelTypeTable(
              new PhpTypeTable(apiConfig.getPackageName()),
              new PhpModelTypeNameConverter(apiConfig.getPackageName()));
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service,
              apiConfig,
              modelTypeTable,
              new PhpSurfaceNamer(apiConfig.getPackageName()),
              new PhpFeatureConfig());
      surfaceDocs.add(transformContext(context, XAPI_TEMPLATE_FILENAME));
    }
    return surfaceDocs;
  }

  protected GapicCodePathMapper getPathMapper() {
    return PhpGapicCodePathMapper.defaultInstance();
  }

  protected ImportTypeTransformer getImportTypeTransformer() {
    return new PhpImportTypeTransformer();
  }

  protected void addApiImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("\\Google\\GAX\\AgentHeaderDescriptor");
    typeTable.saveNicknameFor("\\Google\\GAX\\ApiCallable");
    typeTable.saveNicknameFor("\\Google\\GAX\\CallSettings");
    typeTable.saveNicknameFor("\\Google\\GAX\\GrpcConstants");
    typeTable.saveNicknameFor("\\Google\\GAX\\GrpcCredentialsHelper");
    typeTable.saveNicknameFor("\\Google\\GAX\\PathTemplate");
    typeTable.saveNicknameFor("\\Google\\GAX\\ValidationException");

    if (context.getInterfaceConfig().hasPageStreamingMethods()) {
      typeTable.saveNicknameFor("\\Google\\GAX\\PageStreamingDescriptor");
    }

    if (context.getInterfaceConfig().hasLongRunningOperations()) {
      typeTable.saveNicknameFor("Google\\Longrunning\\OperationsClient");
    }
  }
}
