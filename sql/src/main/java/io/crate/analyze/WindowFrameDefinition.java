/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.analyze;

import io.crate.sql.tree.WindowFrame.Type;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;

public class WindowFrameDefinition implements Writeable {

    private final Type type;
    private final FrameBoundDefinition start;
    @Nullable
    private final FrameBoundDefinition end;

    public WindowFrameDefinition(StreamInput in) throws IOException {
        type = in.readEnum(Type.class);
        start = new FrameBoundDefinition(in);
        end = in.readOptionalWriteable(FrameBoundDefinition::new);
    }

    public WindowFrameDefinition(Type type, FrameBoundDefinition start, @Nullable FrameBoundDefinition end) {
        this.type = type;
        this.start = start;
        this.end = end;
    }

    public Type type() {
        return type;
    }

    public FrameBoundDefinition start() {
        return start;
    }

    @Nullable
    public FrameBoundDefinition end() {
        return end;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeEnum(type);
        start.writeTo(out);
        out.writeOptionalWriteable(end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WindowFrameDefinition that = (WindowFrameDefinition) o;
        return type == that.type &&
               Objects.equals(start, that.start) &&
               Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, start, end);
    }

    @Override
    public String toString() {
        return "WindowFrame{" +
               "type=" + type +
               ", start=" + start +
               ", end=" + end +
               '}';
    }
}
