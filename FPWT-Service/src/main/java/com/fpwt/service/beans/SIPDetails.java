package com.fpwt.service.beans;

/**
 * @author c-NikunjP
 *
 */
public class SIPDetails {
	private double sipValue;
	private double finalValue;
	private double inflationValue;
	private double expectedReturn;

	public double getSipValue() {
		return sipValue;
	}

	public void setSipValue(double sipValue) {
		this.sipValue = sipValue;
	}

	public double getFinalValue() {
		return finalValue;
	}

	public void setFinalValue(double finalValue) {
		this.finalValue = finalValue;
	}

	public double getInflationValue() {
		return inflationValue;
	}

	public void setInflationValue(double inflationValue) {
		this.inflationValue = inflationValue;
	}

	public double getExpectedReturn() {
		return expectedReturn;
	}

	public void setExpectedReturn(double expectedReturn) {
		this.expectedReturn = expectedReturn;
	}

	@Override
	public String toString() {
		return "SIPDetails [sipValue=" + sipValue + ", finalValue=" + finalValue + ", inflationValue=" + inflationValue
				+ ", expectedReturn=" + expectedReturn + "]";
	}

}
