/*
 * Copyright 2016 The BSOA Project
 *
 * The BSOA Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.bsoa.rpc.common.struct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 比较两个list的不同，列出差异部分：包括左侧独有，右侧独有，双方都有
 * <p>
 * Created by zhangg on 2016/7/16 00:18.
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ListDifference<T> {

    /**
     * The Only on left.
     */
    private List<T> onlyOnLeft;

    /**
     * The Only on right.
     */
    private List<T> onlyOnRight;

    /**
     * The On both.
     */
    private List<T> onBoth;

    /**
     * Difference list difference.
     *
     * @param left  the left
     * @param right the right
     */
    public ListDifference(List<? extends T> left, List<? extends T> right) {

        boolean switched = false;
        if (left.size() < right.size()) { // 做优化，比较大小，只遍历少的
            List<? extends T> tmp = left;
            left = right;
            right = tmp;
            switched = true;
        }

        List<T> onlyOnLeft = new ArrayList<T>();
        List<T> onlyOnRight = new ArrayList<T>(right);
        List<T> onBoth = new ArrayList<T>();

        for (T leftValue : left) {
            if (right.contains(leftValue)) {
                onlyOnRight.remove(leftValue);
                onBoth.add(leftValue);
            } else {
                onlyOnLeft.add(leftValue);
            }
        }
        this.onlyOnLeft = Collections.unmodifiableList(switched ? onlyOnRight : onlyOnLeft);
        this.onlyOnRight = Collections.unmodifiableList(switched ? onlyOnLeft : onlyOnRight);
        this.onBoth = Collections.unmodifiableList(onBoth);
    }

    /**
     * Are equal.
     *
     * @return the boolean
     */
    public boolean areEqual() {
        return onlyOnLeft.isEmpty() && onlyOnRight.isEmpty();
    }

    /**
     * Gets only on left.
     *
     * @return the only on left
     */
    public List<T> getOnlyOnLeft() {
        return onlyOnLeft;
    }

    /**
     * Gets only on right.
     *
     * @return the only on right
     */
    public List<T> getOnlyOnRight() {
        return onlyOnRight;
    }

    /**
     * Gets on both.
     *
     * @return the on both
     */
    public List<T> getOnBoth() {
        return onBoth;
    }

}