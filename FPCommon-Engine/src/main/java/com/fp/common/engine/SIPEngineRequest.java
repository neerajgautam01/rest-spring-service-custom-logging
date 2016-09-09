package com.fp.common.engine;

import java.util.List;

/**
 * @author c-NikunjP
 *
 */
public class SIPEngineRequest {
	private Integer riskScore;
	private List<GoalDetails> goalDetails;
	private String versionId;

	public Integer getRiskScore() {
		return riskScore;
	}

	public void setRiskScore(Integer riskScore) {
		this.riskScore = riskScore;
	}

	public List<GoalDetails> getGoalDetails() {
		return goalDetails;
	}

	public void setGoalDetails(List<GoalDetails> goalDetails) {
		this.goalDetails = goalDetails;
	}

	public String getVersionId() {
		return versionId;
	}

	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}

	@Override
	public String toString() {
		return "SIPEngineRequest [riskScore=" + riskScore + ", goalDetails=" + goalDetails + ", versionId=" + versionId
				+ "]";
	}

}
