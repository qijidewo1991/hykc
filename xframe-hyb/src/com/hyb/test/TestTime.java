package com.hyb.test;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;


public class TestTime {
	public static void main(String[] args){
		//BigInteger a=BigInteger.valueOf(1505513218974L);
		long abcd=1505513218974L;
		long last=1505513248003L;
		System.out.println(TimeUnit.MILLISECONDS.toSeconds(abcd-last));
	}
}
