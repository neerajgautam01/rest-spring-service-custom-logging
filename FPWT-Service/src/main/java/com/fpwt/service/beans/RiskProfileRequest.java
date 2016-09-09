package com.fpwt.service.beans;

public class RiskProfileRequest {
	private RPEngineRequest rpRequest;
	private ProfileDetails profileDetails;
	private String rmid;

	public ProfileDetails getProfileDetails() {
		return this.profileDetails;
	}

	public void setProfileDetails(ProfileDetails profileDetails) {
		this.profileDetails = profileDetails;
	}

	public RPEngineRequest getRpRequest() {
		return this.rpRequest;
	}

	public void setRpRequest(RPEngineRequest rpRequest) {
		this.rpRequest = rpRequest;
	}

	public String getRmid() {
		return rmid;
	}

	public void setRmid(String rmid) {
		this.rmid = rmid;
	}
}