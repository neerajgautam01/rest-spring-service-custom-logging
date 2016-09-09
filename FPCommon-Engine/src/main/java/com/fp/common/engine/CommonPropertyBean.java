package com.fp.common.engine;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CommonPropertyBean {

	@Value("${template.excel.path}")
	private String templatePath;

	@Value("${scoring.excel.path}")
	private String scoringPath;

	@Value("${http.riskProfile.server.context}")
	private String httpRPServerContext;

	@Value("${http.server.port}")
	private String httpServerPort;

	@Value("${http.server.ip}")
	private String httpServerIP;

	@Value("${http.assetAllocation.server.context}")
	private String httpAssetServerContext;

	public String getHttpRPServerContext() {
		return this.httpRPServerContext;
	}

	public void setHttpRPServerContext(String httpRPServerContext) {
		this.httpRPServerContext = httpRPServerContext;
	}

	public String getHttpServerPort() {
		return this.httpServerPort;
	}

	public void setHttpServerPort(String httpServerPort) {
		this.httpServerPort = httpServerPort;
	}

	public String getTemplatePath() {
		return this.templatePath;
	}

	public void setTemplatePath(String rpTemplateExcelPath) {
		this.templatePath = rpTemplateExcelPath;
	}

	public String getHttpServerIP() {
		return this.httpServerIP;
	}

	public void setHttpServerIP(String httpServerIP) {
		this.httpServerIP = httpServerIP;
	}

	public String getScoringPath() {
		return scoringPath;
	}

	public void setScoringPath(String scoringPath) {
		this.scoringPath = scoringPath;
	}

	public String getHttpAssetServerContext() {
		return httpAssetServerContext;
	}

	public void setHttpAssetServerContext(String httpAssetServerContext) {
		this.httpAssetServerContext = httpAssetServerContext;
	}

	@Override
	public String toString() {
		return "RislProfilePropertyBean [templatePath=" + templatePath + ", scoringPath=" + scoringPath + ", httpServerContext=" + httpRPServerContext
				+ ", httpServerPort=" + httpServerPort + ", httpServerIP=" + httpServerIP + ", httpAssetServerContext=" + httpAssetServerContext
				+ "]";
	}

}