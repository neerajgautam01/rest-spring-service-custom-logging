package com.fpwt.service.beans;

/**
 * @author c-NikunjP
 *
 */
public class AssetAllocationResponse {
	private String base64Value;

	public String getBase64Value() {
		return base64Value;
	}

	public void setBase64Value(String base64Value) {
		this.base64Value = base64Value;
	}

	@Override
	public String toString() {
		return "AssetAllocationResponse [base64Value=" + base64Value + "]";
	}

}
