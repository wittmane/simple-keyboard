package rkr.simplekeyboard.inputmethod.latin.utils;

import org.junit.Test;

import rkr.simplekeyboard.inputmethod.latin.utils.RangeList.Range;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RangeListTests {
    @Test
    public void singleRanges() {
        RangeList rangeList = new RangeList();
        rangeList.add(12, 42);
        assertEquals("min", 12, rangeList.min());
        assertEquals("max", 42, rangeList.max());
        Range[] gaps = rangeList.gaps();
        assertEquals("gaps length", 0, gaps.length);
    }

    @Test
    public void inOrderRanges() {
        RangeList rangeList = new RangeList();
        rangeList.add(0, 2);
        rangeList.add(5, 13);
        rangeList.add(20, 25);
        assertEquals("min", 0, rangeList.min());
        assertEquals("max", 25, rangeList.max());
        Range[] gaps = rangeList.gaps();
        assertEquals("gaps length", 2, gaps.length);
        assertEquals("gap 0 start", 3, gaps[0].start());
        assertEquals("gap 0 end", 4, gaps[0].end());
        assertEquals("gap 1 start", 14, gaps[1].start());
        assertEquals("gap 1 end", 19, gaps[1].end());
    }

    @Test
    public void outOfOrderRanges() {
        RangeList rangeList = new RangeList();
        rangeList.add(200, 9001);
        rangeList.add(8, 16);
        rangeList.add(-20, 1);
        assertEquals("min", -20, rangeList.min());
        assertEquals("max", 9001, rangeList.max());
        Range[] gaps = rangeList.gaps();
        assertEquals("gaps length", 2, gaps.length);
        assertEquals("gap 0 start", 2, gaps[0].start());
        assertEquals("gap 0 end", 7, gaps[0].end());
        assertEquals("gap 1 start", 17, gaps[1].start());
        assertEquals("gap 1 end", 199, gaps[1].end());
    }

    @Test
    public void extendBeginning() {
        RangeList rangeList = new RangeList();
        rangeList.add(5, 10);
        rangeList.add(2, 4);
        assertEquals("min", 2, rangeList.min());
        assertEquals("max", 10, rangeList.max());
        Range[] gaps = rangeList.gaps();
        assertEquals("gaps length", 0, gaps.length);
    }

    @Test
    public void overlapFirst() {
        RangeList rangeList = new RangeList();
        rangeList.add(2, 12);
        rangeList.add(20, 25);
        rangeList.add(-1, 13);
        assertEquals("min", -1, rangeList.min());
        assertEquals("max", 25, rangeList.max());
        Range[] gaps = rangeList.gaps();
        assertEquals("gaps length", 1, gaps.length);
        assertEquals("gap 0 start", 14, gaps[0].start());
        assertEquals("gap 0 end", 19, gaps[0].end());
    }

    @Test
    public void extendEnd() {
        RangeList rangeList = new RangeList();
        rangeList.add(1, 4);
        rangeList.add(8, 10);
        rangeList.add(10, 15);
        assertEquals("min", 1, rangeList.min());
        assertEquals("max", 15, rangeList.max());
        Range[] gaps = rangeList.gaps();
        assertEquals("gaps length", 1, gaps.length);
        assertEquals("gap 0 start", 5, gaps[0].start());
        assertEquals("gap 0 end", 7, gaps[0].end());
    }

    @Test
    public void overlapMultipleMiddle() {
        RangeList rangeList = new RangeList();
        rangeList.add(0, 5);
        rangeList.add(10, 15);
        rangeList.add(20, 25);
        rangeList.add(30, 35);
        rangeList.add(40, 45);
        rangeList.add(7, 29);
        assertEquals("min", 0, rangeList.min());
        assertEquals("max", 45, rangeList.max());
        Range[] gaps = rangeList.gaps();
        assertEquals("gaps length", 2, gaps.length);
        assertEquals("gap 0 start", 6, gaps[0].start());
        assertEquals("gap 0 end", 6, gaps[0].end());
        assertEquals("gap 1 start", 36, gaps[1].start());
        assertEquals("gap 1 end", 39, gaps[1].end());
    }

    @Test
    public void overlapAll() {
        RangeList rangeList = new RangeList();
        rangeList.add(0, 5);
        rangeList.add(10, 15);
        rangeList.add(20, 25);
        rangeList.add(25, 30);
        rangeList.add(35, 40);
        rangeList.add(-1, 100);
        assertEquals("min", -1, rangeList.min());
        assertEquals("max", 100, rangeList.max());
        Range[] gaps = rangeList.gaps();
        assertEquals("gaps length", 0, gaps.length);
    }

    @Test
    public void fillGapExact() {
        RangeList rangeList = new RangeList();
        rangeList.add(0, 5);
        rangeList.add(10, 15);
        rangeList.add(6, 9);
        assertEquals("min", 0, rangeList.min());
        assertEquals("max", 15, rangeList.max());
        Range[] gaps = rangeList.gaps();
        assertEquals("gaps length", 0, gaps.length);
    }

    @Test
    public void checkRangeBefore() {
        RangeList rangeList = new RangeList();
        rangeList.add(0, 5);
        rangeList.add(10, 15);
        assertFalse("in range", rangeList.isRangeIncluded(-5, -1));
    }

    @Test
    public void checkRangeBetween() {
        RangeList rangeList = new RangeList();
        rangeList.add(0, 5);
        rangeList.add(10, 15);
        assertFalse("in range", rangeList.isRangeIncluded(6, 9));
    }

    @Test
    public void checkRangeAfter() {
        RangeList rangeList = new RangeList();
        rangeList.add(0, 5);
        rangeList.add(10, 15);
        assertFalse("in range", rangeList.isRangeIncluded(16, 20));
    }

    @Test
    public void checkRangeSpanGap() {
        RangeList rangeList = new RangeList();
        rangeList.add(0, 5);
        rangeList.add(10, 15);
        assertFalse("in range", rangeList.isRangeIncluded(5, 10));
    }

    @Test
    public void checkRangePartialOverlap() {
        RangeList rangeList = new RangeList();
        rangeList.add(0, 5);
        rangeList.add(10, 15);
        assertFalse("in range", rangeList.isRangeIncluded(1, 6));
    }

    @Test
    public void checkRangeMatch() {
        RangeList rangeList = new RangeList();
        rangeList.add(0, 5);
        rangeList.add(10, 15);
        assertTrue("in range", rangeList.isRangeIncluded(0, 5));
    }

    @Test
    public void checkRangeInside() {
        RangeList rangeList = new RangeList();
        rangeList.add(0, 5);
        rangeList.add(10, 15);
        assertTrue("in range", rangeList.isRangeIncluded(1, 4));
    }
}
