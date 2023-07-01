/*
 * Copyright (C) 2023 Eli Wittman
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

package rkr.simplekeyboard.inputmethod.latin.utils;

import java.util.NoSuchElementException;

public class RangeList {
    public static class Range implements Comparable<Range> {
        private int mStart;
        private int mEnd;

        public Range(int start, int end) {
            mStart = start;
            mEnd = end;
        }

        public int start() {
            return mStart;
        }

        public int end() {
            return mEnd;
        }

        @Override
        public int compareTo(Range range) {
            int startCompare = Integer.compare(mStart, range.mStart);
            return startCompare != 0 ? startCompare : Integer.compare(mEnd, range.mEnd);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Range)) {
                return false;
            }
            Range other = (Range) o;
            return mStart == other.mStart && mEnd == other.mEnd;
        }
    }

    private static class Node {
        Range mValue;
        Node mNext;
        Node(final Range value) {
            mValue = value;
        }
    }

    private Node mHead;
    private Node mTail;
    private int mCount;

    /**
     * Add a range to the list. If this range is adjacent to or overlaps other ranges, the ranges
     * will be merged in the list.
     * @param start The start of the range to add (inclusive).
     * @param end The end of the range to add (inclusive).
     */
    public void add(int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException("Invalid range: start=" + start + ", end=" + end);
        }

        // skip past the non-adjacent ranges before the new one to find where it should be added
        Node previous = null;
        Node current = mHead;
        while (current != null && start > current.mValue.mEnd + 1) {
            previous = current;
            current = current.mNext;
        }

        if (current != null && end + 1 >= current.mValue.mStart) {
            // merge with current
            current.mValue.mStart = Math.min(current.mValue.mStart, start);
            current.mValue.mEnd = Math.max(current.mValue.mEnd, end);
            // merge with any later ranges that also overlap or are adjacent now
            while (current.mNext != null && end + 1 >= current.mNext.mValue.mStart) {
                current.mValue.mEnd = Math.max(current.mValue.mEnd, current.mNext.mValue.mEnd);
                current.mNext = current.mNext.mNext;
                mCount--;
            }
            if (current.mNext == null) {
                mTail = current;
            }
        } else {
            // insert
            Node node = new Node(new Range(start, end));
            if (previous == null) {
                mHead = node;
            } else {
                previous.mNext = node;
            }
            if (current == null) {
                mTail = node;
            } else {
                node.mNext = current;
            }
            mCount++;
        }
    }

    /**
     * Get the number of ranges in the list.
     * @return The number of ranges in the list.
     */
    public int size() {
        return mCount;
    }

    /**
     * Check if a range is included in any ranges in the list.
     * @param start The range start end to check.
     * @param end The range end to check.
     * @return Whether the range is included.
     */
    public boolean isRangeIncluded(int start, int end) {
        if (mHead == null) {
            return false;
        }
        Node current = mHead;
        while (current != null) {
            if (current.mValue.mStart <= start && end <= current.mValue.mEnd) {
                return true;
            }
            if (start < current.mValue.mStart) {
                // already passed where the range should have been
                return false;
            }
            current = current.mNext;
        }
        return false;
    }

    /**
     * Get the minimum range start from the list. This can only be called if the list isn't empty.
     * @return The minimum range start from the list.
     */
    public int min() {
        if (mHead == null) {
            throw new NoSuchElementException("No ranges in list");
        }
        return mHead.mValue.mStart;
    }

    /**
     * Get the maximum range start from the list. This can only be called if the list isn't empty.
     * @return The maximum range start from the list.
     */
    public int max() {
        if (mTail == null) {
            throw new NoSuchElementException("No ranges in list");
        }
        return mTail.mValue.mEnd;
    }

    /**
     * Get the list of gaps between the ranges in the list.
     * @return The list of gaps between the ranges in the list.
     */
    public Range[] gaps() {
        Range[] gaps = new Range[Math.max(0, mCount - 1)];
        Node previous;
        Node next = mHead;
        int i = 0;
        while (next != null && next.mNext != null) {
            previous = next;
            next = next.mNext;
            gaps[i++] = new Range(previous.mValue.mEnd + 1, next.mValue.mStart - 1);
        }
        return gaps;
    }
}
