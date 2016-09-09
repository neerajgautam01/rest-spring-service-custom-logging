package com.fpwt.service.beans;

/**
 * @author c-NikunjP
 *
 */
public class PerformanceBean {
	private String indexName;
	private String sType;
	private String rtaCode;
	private double threeMonthReturn;
	private double sixMonthReturn;
	private double oneYearReturn;
	private double threeYearReturn;
	private double fiveYearReturn;

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public String getsType() {
		return sType;
	}

	public void setsType(String sType) {
		this.sType = sType;
	}

	public String getRtaCode() {
		return rtaCode;
	}

	public void setRtaCode(String rtaCode) {
		this.rtaCode = rtaCode;
	}

	public double getThreeMonthReturn() {
		return threeMonthReturn;
	}

	public void setThreeMonthReturn(double threeMonthReturn) {
		this.threeMonthReturn = threeMonthReturn;
	}

	public double getSixMonthReturn() {
		return sixMonthReturn;
	}

	public void setSixMonthReturn(double sixMonthReturn) {
		this.sixMonthReturn = sixMonthReturn;
	}

	public double getOneYearReturn() {
		return oneYearReturn;
	}

	public void setOneYearReturn(double oneYearReturn) {
		this.oneYearReturn = oneYearReturn;
	}

	public double getThreeYearReturn() {
		return threeYearReturn;
	}

	public void setThreeYearReturn(double threeYearReturn) {
		this.threeYearReturn = threeYearReturn;
	}

	public double getFiveYearReturn() {
		return fiveYearReturn;
	}

	public void setFiveYearReturn(double fiveYearReturn) {
		this.fiveYearReturn = fiveYearReturn;
	}

	@Override
	public String toString() {
		return "PerformanceBean [indexName=" + indexName + ", sType=" + sType + ", rtaCode=" + rtaCode
				+ ", threeMonthReturn=" + threeMonthReturn + ", sixMonthReturn=" + sixMonthReturn + ", oneYearReturn="
				+ oneYearReturn + ", threeYearReturn=" + threeYearReturn + ", fiveYearReturn=" + fiveYearReturn + "]";
	}

}
