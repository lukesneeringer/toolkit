/* Copyright 2017 Google Inc
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

import com.google.api.codegen.transformer.ImportSectionTransformer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.viewmodel.ImportFileView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.common.collect.ImmutableList;
import java.util.Map;

public class PhpImportSectionTransformer implements ImportSectionTransformer {
  @Override
  public ImportSectionView generateImportSection(SurfaceTransformerContext context) {
    ImmutableList.Builder<ImportFileView> appImports = ImmutableList.builder();
    for (Map.Entry<String, TypeAlias> entry : context.getTypeTable().getImports().entrySet()) {
      String key = entry.getKey();
      TypeAlias alias = entry.getValue();
      // Remove leading backslash because it is not required by PHP use statements
      String fullName = key.startsWith("\\") ? key.substring(1) : key;
      ImportTypeView.Builder imp = ImportTypeView.newBuilder();
      imp.fullName(fullName);
      imp.nickname(alias.getNickname());
      imp.type(alias.getImportType());
      appImports.add(ImportFileView.newBuilder().types(ImmutableList.of(imp.build())).build());
    }
    return ImportSectionView.newBuilder().appImports(appImports.build()).build();
  }
}