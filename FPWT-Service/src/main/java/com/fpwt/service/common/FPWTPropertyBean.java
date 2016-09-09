package com.fpwt.service.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FPWTPropertyBean {

	@Value("${template.excel.path}")
	private String templatePath;

	@Value("${http.riskProfile.server.context}")
	private String httpServerContext;

	@Value("${http.server.port}")
	private String httpServerPort;

	@Value("${http.server.ip}")
	private String httpServerIP;

	@Value("${http.assetAllocation.server.context}")
	private String httpAssetServerContext;

	@Value("${EncryptionKey}")
	private String encryptionKey;

	public String getHttpServerContext() {
		return this.httpServerContext;
	}

	public void setHttpServerContext(String httpServerContext) {
		this.httpServerContext = httpServerContext;
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

	public String getHttpAssetServerContext() {
		return httpAssetServerContext;
	}

	public void setHttpAssetServerContext(String httpAssetServerContext) {
		this.httpAssetServerContext = httpAssetServerContext;
	}

	public String getEncryptionKey() {
		return encryptionKey;
	}

	public void setEncryptionKey(String encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	@Override
	public String toString() {
		return "FPWTPropertyBean [templatePath=" + templatePath + ", httpServerContext=" + httpServerContext + ", httpServerPort=" + httpServerPort
				+ ", httpServerIP=" + httpServerIP + ", httpAssetServerContext=" + httpAssetServerContext + "]";
	}

}