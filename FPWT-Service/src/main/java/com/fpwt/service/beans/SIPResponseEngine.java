package com.fpwt.service.beans;

import java.util.HashMap;
import java.util.Map;

/**
 * @author c-NikunjP
 *
 */
public class SIPResponseEngine {
	private String responseCode;
	Map<String, SIPDetails> sipDetails = new HashMap<String, SIPDetails>();

	public String getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(String responseCode) {
		this.responseCode = responseCode;
	}

	public Map<String, SIPDetails> getSipDetails() {
		return sipDetails;
	}

	public void setSipDetails(Map<String, SIPDetails> sipDetails) {
		this.sipDetails = sipDetails;
	}

	@Override
	public String toString() {
		return "SIPResponseEngine [responseCode=" + responseCode + ", sipDetails=" + sipDetails + "]";
	}

}
