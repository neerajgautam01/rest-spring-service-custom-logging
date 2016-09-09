package com.fpwt.service.beans;

public class RPEngineResponse {
	private String responseCode;
	private String riskScore;

	public String getResponseCode() {
		return this.responseCode;
	}

	public void setResponseCode(String responseCode) {
		this.responseCode = responseCode;
	}

	public String getRiskScore() {
		return this.riskScore;
	}

	public void setRiskScore(String riskScore) {
		this.riskScore = riskScore;
	}
}