package com.fpwt.service.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author c-NikunjP
 *
 */
public class AssetFolioData {
	@JsonProperty("SCHCODE")
	private String schcode;
	@JsonProperty("SCH_NAME")
	private String sch_name;
	@JsonProperty("BALANCE_UNITS")
	private double balance_units;
	@JsonProperty("CURRENT_VALUE")
	private double current_value;
	@JsonProperty("COST_VALUE")
	private double cost_value;
	@JsonProperty("DIVIDENDS_REINVESTED")
	private double dividends_reinvested;
	@JsonProperty("GAIN_LOSS")
	private double gain_loss;
	@JsonProperty("GAIN_LOSS_INDICATOR")
	private String gain_loss_indicator;
	@JsonProperty("ASSET_CLASS")
	private String asset_class;
	@JsonProperty("XIRR")
	private String xirr;

	public String getSchcode() {
		return schcode;
	}

	public void setSchcode(String schcode) {
		this.schcode = schcode;
	}

	public String getSch_name() {
		return sch_name;
	}

	public void setSch_name(String sch_name) {
		this.sch_name = sch_name;
	}

	public double getBalance_units() {
		return balance_units;
	}

	public void setBalance_units(double balance_units) {
		this.balance_units = balance_units;
	}

	public double getCurrent_value() {
		return current_value;
	}

	public void setCurrent_value(double current_value) {
		this.current_value = current_value;
	}

	public double getCost_value() {
		return cost_value;
	}

	public void setCost_value(double cost_value) {
		this.cost_value = cost_value;
	}

	public double getDividends_reinvested() {
		return dividends_reinvested;
	}

	public void setDividends_reinvested(double dividends_reinvested) {
		this.dividends_reinvested = dividends_reinvested;
	}

	public double getGain_loss() {
		return gain_loss;
	}

	public void setGain_loss(double gain_loss) {
		this.gain_loss = gain_loss;
	}

	public String getGain_loss_indicator() {
		return gain_loss_indicator;
	}

	public void setGain_loss_indicator(String gain_loss_indicator) {
		this.gain_loss_indicator = gain_loss_indicator;
	}

	public String getAsset_class() {
		return asset_class;
	}

	public void setAsset_class(String asset_class) {
		this.asset_class = asset_class;
	}

	public String getXirr() {
		return xirr;
	}

	public void setXirr(String xirr) {
		this.xirr = xirr;
	}

	@Override
	public String toString() {
		return "AssetFolioData [schcode=" + schcode + ", sch_name=" + sch_name + ", balance_units=" + balance_units
				+ ", current_value=" + current_value + ", cost_value=" + cost_value + ", dividends_reinvested="
				+ dividends_reinvested + ", gain_loss=" + gain_loss + ", gain_loss_indicator=" + gain_loss_indicator
				+ ", asset_class=" + asset_class + ", xirr=" + xirr + "]";
	}

}
