package com.fpwt.service.beans;

public class RiskProfileResponse {
	private String base64Value;
	private String level;
	private String score;

	public String getBase64Value() {
		return this.base64Value;
	}

	public void setBase64Value(String base64Value) {
		this.base64Value = base64Value;
	}

	public String getLevel() {
		return this.level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getScore() {
		return this.score;
	}

	public void setScore(String score) {
		this.score = score;
	}
}