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

package org.apache.iotdb.db.storageengine.dataregion.wal.buffer;

import org.apache.iotdb.db.conf.IoTDBConfig;
import org.apache.iotdb.db.conf.IoTDBDescriptor;
import org.apache.iotdb.db.queryengine.plan.planner.plan.node.write.InsertTabletNode;
import org.apache.iotdb.db.storageengine.dataregion.wal.utils.WALMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** This entry class stores info for persistence. */
public class WALInfoEntry extends WALEntry {
  private static final IoTDBConfig config = IoTDBDescriptor.getInstance().getConfig();
  // wal entry type 1 byte, memTable id 8 bytes
  public static final int FIXED_SERIALIZED_SIZE = Byte.BYTES + Long.BYTES;

  // extra info for InsertTablet type value
  private TabletInfo tabletInfo;

  public WALInfoEntry(long memTableId, WALEntryValue value, boolean wait) {
    super(memTableId, value, wait);
  }

  public WALInfoEntry(long memTableId, WALEntryValue value) {
    this(memTableId, value, config.getWalMode() == WALMode.SYNC);
    if (value instanceof InsertTabletNode) {
      tabletInfo =
          new TabletInfo(
              Collections.singletonList(new int[] {0, ((InsertTabletNode) value).getRowCount()}));
    }
  }

  public WALInfoEntry(long memTableId, InsertTabletNode value, List<int[]> tabletRangeList) {
    this(memTableId, value, config.getWalMode() == WALMode.SYNC);
    tabletInfo = new TabletInfo(tabletRangeList);
  }

  WALInfoEntry(WALEntryType type, long memTableId, WALEntryValue value) {
    super(type, memTableId, value, false);
    if (value instanceof InsertTabletNode) {
      tabletInfo =
          new TabletInfo(
              Collections.singletonList(new int[] {0, ((InsertTabletNode) value).getRowCount()}));
    }
  }

  @Override
  public int serializedSize() {
    return FIXED_SERIALIZED_SIZE + (value == null ? 0 : value.serializedSize());
  }

  @Override
  public void serialize(IWALByteBufferView buffer) {
    buffer.put(type.getCode());
    buffer.putLong(memTableId);
    switch (type) {
      case INSERT_TABLET_NODE:
        ((InsertTabletNode) value).serializeToWAL(buffer, tabletInfo.tabletRangeList);
        break;
      case INSERT_ROW_NODE:
      case INSERT_ROWS_NODE:
      case DELETE_DATA_NODE:
      case MEMORY_TABLE_SNAPSHOT:
      case CONTINUOUS_SAME_SEARCH_INDEX_SEPARATOR_NODE:
        value.serializeToWAL(buffer);
        break;
      case MEMORY_TABLE_CHECKPOINT:
        throw new RuntimeException("Cannot serialize checkpoint to wal files.");
      default:
        throw new RuntimeException("Unsupported wal entry type " + type);
    }
  }

  private static class TabletInfo {
    // ranges of insert tablet
    private final List<int[]> tabletRangeList;

    public TabletInfo(List<int[]> tabletRangeList) {
      this.tabletRangeList = new ArrayList<>(tabletRangeList);
    }

    @Override
    public int hashCode() {
      return Objects.hash(tabletRangeList);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof TabletInfo)) {
        return false;
      }
      TabletInfo that = (TabletInfo) obj;
      if (this.tabletRangeList.size() != that.tabletRangeList.size()) {
        return false;
      }

      for (int i = 0; i < tabletRangeList.size(); i++) {
        if (!Arrays.equals(this.tabletRangeList.get(i), that.tabletRangeList.get(i))) {
          return false;
        }
      }
      return true;
    }
  }

  @Override
  public boolean isSignal() {
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), tabletInfo);
  }

  @Override
  public boolean equals(Object obj) {
    if (!super.equals(obj)) {
      return false;
    }
    WALInfoEntry other = (WALInfoEntry) obj;
    return Objects.equals(this.tabletInfo, other.tabletInfo);
  }
}
