package vn.ipz.phageipz.engine.qc.util;

import java.util.Vector;
import vn.ipz.phageipz.config.PhageIPZProperties; 

public class BaseGroup {

    private int lowerCount;
    private int upperCount;

    public static BaseGroup[] makeBaseGroups(int maxLength, PhageIPZProperties properties) {
        // Lấy cấu hình từ properties thay vì FastQCConfig.getInstance()
        if (properties.getAnalysis().getMinLength() > maxLength) {
            maxLength = properties.getAnalysis().getMinLength();
        }
        
        if (properties.getAnalysis().isNogroup()) {
            return makeUngroupedGroups(maxLength);
        } else if (properties.getAnalysis().isExpgroup()) {
            return makeExponentialBaseGroups(maxLength);
        } else {
            return makeLinearBaseGroups(maxLength);
        }
    }

    public static BaseGroup[] makeUngroupedGroups(int maxLength) {
        int startingBase = 1;
        Vector<BaseGroup> groups = new Vector<>();
        while (startingBase <= maxLength) {
            BaseGroup bg = new BaseGroup(startingBase, startingBase);
            groups.add(bg);
            startingBase++;
        }
        return groups.toArray(new BaseGroup[0]);
    }

    public static BaseGroup[] makeExponentialBaseGroups(int maxLength) {
        int startingBase = 1;
        int interval = 1;
        Vector<BaseGroup> groups = new Vector<>();
        while (startingBase <= maxLength) {
            int endBase = startingBase + (interval - 1);
            if (endBase > maxLength) endBase = maxLength;
            groups.add(new BaseGroup(startingBase, endBase));
            startingBase += interval;
            if (startingBase == 10 && maxLength > 75) interval = 5;
            if (startingBase == 50 && maxLength > 200) interval = 10;
            if (startingBase == 100 && maxLength > 300) interval = 50;
            if (startingBase == 500 && maxLength > 1000) interval = 100;
            if (startingBase == 1000 && maxLength > 2000) interval = 500;
        }
        return groups.toArray(new BaseGroup[0]);
    }

    private static int getLinearInterval(int length) {
        int[] baseValues = {2, 5, 10};
        int multiplier = 1;
        while (true) {
            for (int baseValue : baseValues) {
                int interval = baseValue * multiplier;
                int groupCount = 9 + ((length - 9) / interval) + ((length - 9) % interval != 0 ? 1 : 0);
                if (groupCount < 75) return interval;
            }
            multiplier *= 10;
            if (multiplier == 10000000) throw new IllegalStateException("Interval not found for length: " + length);
        }
    }

    public static BaseGroup[] makeLinearBaseGroups(int maxLength) {
        if (maxLength <= 75) return makeUngroupedGroups(maxLength);
        int interval = getLinearInterval(maxLength);
        int startingBase = 1;
        Vector<BaseGroup> groups = new Vector<>();
        while (startingBase <= maxLength) {
            int endBase = startingBase + (interval - 1);
            if (startingBase < 10) endBase = startingBase;
            else if (startingBase == 10 && interval > 10) endBase = interval - 1;
            if (endBase > maxLength) endBase = maxLength;
            groups.add(new BaseGroup(startingBase, endBase));
            if (startingBase < 10) startingBase++;
            else if (startingBase == 10 && interval > 10) startingBase = interval;
            else startingBase += interval;
        }
        return groups.toArray(new BaseGroup[0]);
    }

    private BaseGroup(int lowerCount, int upperCount) {
        this.lowerCount = lowerCount;
        this.upperCount = upperCount;
    }

    public int lowerCount() { return lowerCount; }
    public int upperCount() { return upperCount; }
    public boolean containsValue(int value) { return value >= lowerCount && value <= upperCount; }

    @Override
    public String toString() {
        return (lowerCount == upperCount) ? "" + lowerCount : lowerCount + "-" + upperCount;
    }
}