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

package io.crate.sql.tree;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class DecommissionNode extends Statement {

    private final Expression nodeIdOrName;

    public DecommissionNode(Expression nodeIdOrName) {
        this.nodeIdOrName = nodeIdOrName;
    }

    public Expression nodeIdOrName() {
        return nodeIdOrName;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nodeIdOrName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        DecommissionNode that = (DecommissionNode) obj;
        return nodeIdOrName.equals(that.nodeIdOrName);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("nodeIdOrName", nodeIdOrName).toString();
    }

    @Override
    public <R, C> R accept(AstVisitor<R, C> visitor, C context) {
        return visitor.visitAlterClusterDecommissionNode(this, context);
    }
}
