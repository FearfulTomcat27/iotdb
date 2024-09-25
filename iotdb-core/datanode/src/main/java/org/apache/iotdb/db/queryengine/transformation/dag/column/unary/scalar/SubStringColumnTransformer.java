/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.db.queryengine.transformation.dag.column.unary.scalar;

import org.apache.iotdb.db.exception.sql.SemanticException;
import org.apache.iotdb.db.queryengine.transformation.dag.column.ColumnTransformer;
import org.apache.iotdb.db.queryengine.transformation.dag.column.unary.UnaryColumnTransformer;

import org.apache.tsfile.block.column.Column;
import org.apache.tsfile.block.column.ColumnBuilder;
import org.apache.tsfile.common.conf.TSFileConfig;
import org.apache.tsfile.read.common.type.Type;
import org.apache.tsfile.utils.BytesUtils;

public class SubStringColumnTransformer extends UnaryColumnTransformer {

  private final int beginPosition;
  private final int endPosition;
  public static final String EMPTY_STRING = "";

  public SubStringColumnTransformer(
      Type returnType, ColumnTransformer childColumnTransformer, int beginPosition, int length) {
    super(returnType, childColumnTransformer);
    if (length < 0) {
      throw new SemanticException(
          "Argument exception,the scalar function substring length must not be less than 0");
    }
    // case which is two args or the result of the beginPosition add length is over
    if (length == Integer.MAX_VALUE || beginPosition > Integer.MAX_VALUE - length) {
      this.endPosition = Integer.MAX_VALUE;
    } else {
      this.endPosition = beginPosition + length - 1;
    }
    this.beginPosition = beginPosition;
  }

  @Override
  protected void doTransform(Column column, ColumnBuilder columnBuilder) {
    for (int i = 0, n = column.getPositionCount(); i < n; i++) {
      if (!column.isNull(i)) {
        String currentValue = column.getBinary(i).getStringValue(TSFileConfig.STRING_CHARSET);
        if (beginPosition > currentValue.length()) {
          throw new SemanticException(
              "Argument exception,the scalar function substring beginPosition must not be greater than the string length");
        } else {
          int maxMin = Math.max(1, beginPosition);
          int minMax = Math.min(currentValue.length(), endPosition);
          if (maxMin > minMax) {
            currentValue = EMPTY_STRING;
          } else {
            currentValue = currentValue.substring(maxMin - 1, minMax);
          }
        }
        columnBuilder.writeBinary(BytesUtils.valueOf(currentValue));
      } else {
        columnBuilder.appendNull();
      }
    }
  }

  @Override
  protected void doTransform(Column column, ColumnBuilder columnBuilder, boolean[] selection) {
    for (int i = 0, n = column.getPositionCount(); i < n; i++) {
      if (selection[i] && !column.isNull(i)) {
        String currentValue = column.getBinary(i).getStringValue(TSFileConfig.STRING_CHARSET);
        if (beginPosition > currentValue.length()) {
          throw new SemanticException(
              "Argument exception,the scalar function substring beginPosition must not be greater than the string length");
        } else {
          int maxMin = Math.max(1, beginPosition);
          int minMax = Math.min(currentValue.length(), endPosition);
          if (maxMin > minMax) {
            currentValue = EMPTY_STRING;
          } else {
            currentValue = currentValue.substring(maxMin - 1, minMax);
          }
        }
        columnBuilder.writeBinary(BytesUtils.valueOf(currentValue));
      } else {
        columnBuilder.appendNull();
      }
    }
  }
}
