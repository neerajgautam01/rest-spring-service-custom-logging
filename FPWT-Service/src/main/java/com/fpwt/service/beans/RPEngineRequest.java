package com.fpwt.service.beans;

import java.util.List;

public class RPEngineRequest {
	private List<QuestionAnswersRequest> questionAnswersList;
	private String versionId;

	public List<QuestionAnswersRequest> getQuestionAnswersList() {
		return this.questionAnswersList;
	}

	public void setQuestionAnswersList(List<QuestionAnswersRequest> questionAnswersList) {
		this.questionAnswersList = questionAnswersList;
	}

	public String getVersionId() {
		return this.versionId;
	}

	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}
}