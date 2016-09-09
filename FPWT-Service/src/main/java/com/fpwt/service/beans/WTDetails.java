/**
 * 
 */
package com.fpwt.service.beans;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author c-NikunjP
 *
 */
public class WTDetails {

	@JsonProperty("ListFolioDtls")
	private ArrayList<AssetFolioData> listFolioDtls;

	public ArrayList<AssetFolioData> getListFolioDtls() {
		return listFolioDtls;
	}

	public void setListFolioDtls(ArrayList<AssetFolioData> listFolioDtls) {
		this.listFolioDtls = listFolioDtls;
	}

	@Override
	public String toString() {
		return "WTDetails [listFolioDtls=" + listFolioDtls + "]";
	}

}
