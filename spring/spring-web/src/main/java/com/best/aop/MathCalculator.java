package com.best.aop;

import org.springframework.stereotype.Component;

@Component
public class MathCalculator {

	public int div(int i,int j){
		System.out.println("MathCalculator...div...");
		return i/j;
	}

}
