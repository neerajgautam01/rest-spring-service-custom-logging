package com.fpwt.service.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author c-NikunjP
 *
 */
public class WTRequest {
	@JsonProperty("STATUS")
	private String status;
	@JsonProperty("COST_VALUE")
	private String cost_value;
	@JsonProperty("MARKET_VALUE")
	private String market_value;
	@JsonProperty("DETAILS")
	private WTDetails details;
	@JsonProperty("INVESTOR_NAME")
	private String investorName;
	@JsonProperty("PAN_NO")
	private String folioNo;
	@JsonProperty("ASON_DATE")
	private String asOnDate;
	@JsonProperty("RMID")
	private String rmid;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCost_value() {
		return cost_value;
	}

	public void setCost_value(String cost_value) {
		this.cost_value = cost_value;
	}

	public String getMarket_value() {
		return market_value;
	}

	public void setMarket_value(String market_value) {
		this.market_value = market_value;
	}

	public WTDetails getDetails() {
		return details;
	}

	public void setDetails(WTDetails details) {
		this.details = details;
	}

	public String getInvestorName() {
		return investorName;
	}

	public void setInvestorName(String investorName) {
		this.investorName = investorName;
	}

	public String getFolioNo() {
		return folioNo;
	}

	public void setFolioNo(String folioNo) {
		this.folioNo = folioNo;
	}

	public String getAsOnDate() {
		return asOnDate;
	}

	public void setAsOnDate(String asOnDate) {
		this.asOnDate = asOnDate;
	}

	public String getRmid() {
		return rmid;
	}

	public void setRmid(String rmid) {
		this.rmid = rmid;
	}

	@Override
	public String toString() {
		return "WTRequest [status=" + status + ", cost_value=" + cost_value + ", market_value=" + market_value
				+ ", details=" + details + ", investorName=" + investorName + ", folioNo=" + folioNo + ", asOnDate="
				+ asOnDate + ", rmid=" + rmid + "]";
	}

}
