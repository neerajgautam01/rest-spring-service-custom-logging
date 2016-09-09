package com.fpwt.service.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author c-NikunjP
 *
 */
public class AssetPieData {
	@JsonProperty("ASSET_CLASS")
	private String asset_class;
	@JsonProperty("PERCENTAGE")
	private String percentage;

	public String getAsset_class() {
		return asset_class;
	}

	public void setAsset_class(String asset_class) {
		this.asset_class = asset_class;
	}

	public String getPercentage() {
		return percentage;
	}

	public void setPercentage(String percentage) {
		this.percentage = percentage;
	}

	@Override
	public String toString() {
		return "AssetPieData [asset_class=" + asset_class + ", percentage=" + percentage + "]";
	}

}
