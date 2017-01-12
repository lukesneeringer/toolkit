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
package com.google.api.codegen.transformer.py;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.gapic.CommonGapicCodePathMapper;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.GapicSurfaceTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.py.PythonTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/**
 * Transformer class to transform a Model representing an API into a Python client that uses that
 * API.
 */
public class PythonGapicSurfaceTransformer extends GapicSurfaceTransformer {
  protected GapicCodePathMapper pathMapper =
      CommonGapicCodePathMapper.newBuilder().setShouldAppendPackage(true).build();

  /**
   * Transform a model and API configuration into a list of views.
   *
   * <p>The `transform(Model, ApiConfig)` is the entry point method for all transformer classes, and
   * will be called externally.
   */
  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> answer = new ArrayList<ViewModel>();

    // Generate a list of services, and get the naming rules for Python.
    Iterable<Interface> services = new InterfaceView().getElementIterable(model);
    PythonSurfaceNamer namer = new PythonSurfaceNamer(apiConfig.getPackageName());
    ModelTypeTable typeTable =
        new ModelTypeTable(
            new PythonTypeTable(apiConfig.getPackageName()),
            new PythonModelTypeNameConverter(apiConfig.getPackageName()));

    // Put together the views for each service.
    // Each one will require a separate context; the context object is sent
    // to all individual transformers to provide the necessary rules for
    // exactly how to do the transformation.
    for (Interface service : services) {
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service, apiConfig, typeTable, namer, new PythonFeatureConfig());
      answer.add(transformContext(context, "py/main.snip"));
    }

    // The enums.py file is a special case for the moment.
    answer.add(new EnumViewModel(model, apiConfig));
    return answer;
  }

  @Override
  public void addApiImports(SurfaceTransformerContext context) {
    // pass
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(ServiceViewModel.SNIPPET_FILE_NAME, EnumViewModel.SNIPPET_FILE_NAME);
  }
}
