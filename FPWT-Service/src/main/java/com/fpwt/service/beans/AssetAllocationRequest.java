package com.fpwt.service.beans;

import java.util.List;

/**
 * @author c-NikunjP
 *
 */
public class AssetAllocationRequest {

	private String rmid;
	private Integer riskScore;
	private String versionId;
	private ProfileDetails profileDetails;
	private List<GoalDetails> goalDetails;

	public String getRmid() {
		return rmid;
	}

	public void setRmid(String rmid) {
		this.rmid = rmid;
	}

	public ProfileDetails getProfileDetails() {
		return profileDetails;
	}

	public void setProfileDetails(ProfileDetails profileDetails) {
		this.profileDetails = profileDetails;
	}

	public String getVersionId() {
		return versionId;
	}

	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}

	public List<GoalDetails> getGoalDetails() {
		return goalDetails;
	}

	public void setGoalDetails(List<GoalDetails> goalDetails) {
		this.goalDetails = goalDetails;
	}

	public Integer getRiskScore() {
		return riskScore;
	}

	public void setRiskScore(Integer riskScore) {
		this.riskScore = riskScore;
	}

	@Override
	public String toString() {
		return "AssetAllocationRequest [rmid=" + rmid + ", riskScore=" + riskScore + ", versionId=" + versionId
				+ ", profileDetails=" + profileDetails + ", goalDetails=" + goalDetails + "]";
	}

}
