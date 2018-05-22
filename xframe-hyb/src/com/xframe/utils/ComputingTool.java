package com.xframe.utils;

import java.math.BigDecimal;

/**
 * @author wdx 
 *
 */
public class ComputingTool {
    /**
     * wdx 解决double加减法的失去精度问题。
     * http://www.cnblogs.com/cblogs/p/double-precision.html
     * @param a 
     * @param b
     * @return 计算结果
     */
	public static double bigDecimalAddition(double a,double b){
        BigDecimal bigDecimal1 = new BigDecimal(Double.toString(a));
        BigDecimal bigDecimal2 = new BigDecimal(Double.toString(b));
        double result=0;
        result=bigDecimal1.add(bigDecimal2).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
        return result;
    }
}
