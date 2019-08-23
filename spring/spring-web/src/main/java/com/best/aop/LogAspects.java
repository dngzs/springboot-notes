package com.best.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * ������
 * @author lfy
 *
 * @Aspect�� ����Spring��ǰ����һ��������
 *
 */
@Aspect
@Component
public class LogAspects {


	@Pointcut("execution(public int com.best.aop.MathCalculator.*(..))")
	public void pointCut(){};


	@Before("pointCut()")
	public void logStart(JoinPoint joinPoint){
		Object[] args = joinPoint.getArgs();
		System.out.println(""+joinPoint.getSignature().getName()+"@Before{"+Arrays.asList(args)+"}");
	}

	@After("com.best.aop.LogAspects.pointCut()")
	public void logEnd(JoinPoint joinPoint){
		System.out.println(""+joinPoint.getSignature().getName()+"@After");
	}


	@AfterReturning(value="pointCut()",returning="result")
	public void logReturn(JoinPoint joinPoint, Object result){
		System.out.println(""+joinPoint.getSignature().getName()+"@AfterReturning:{"+result+"}");
	}

	@AfterThrowing(value="pointCut()",throwing="exception")
	public void logException(JoinPoint joinPoint, Exception exception){
		System.out.println(""+joinPoint.getSignature().getName()+"{"+exception+"}");
	}

}
