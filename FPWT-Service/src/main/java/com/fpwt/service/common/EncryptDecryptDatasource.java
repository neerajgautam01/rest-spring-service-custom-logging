package com.fpwt.service.common;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

@Component
public class EncryptDecryptDatasource extends DriverManagerDataSource {

	@Autowired
	FPWTPropertyBean propertyUtil;

	private static Logger LOGGER = LoggerFactory.getLogger(EncryptDecryptDatasource.class.getName());

	@Override
	public String getPassword() {
		String password = super.getPassword();
		return decode(password);
	}

	/**
	 * Method to decrypt the encoded password
	 * 
	 * @param password
	 * @return
	 */
	private String decode(String password) {

		LOGGER.info("Starting decryption...");
		StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
		encryptor.setPassword(propertyUtil.getEncryptionKey());
		String decryptedPropertyValue = encryptor.decrypt(password);
		LOGGER.info("Done.");
		return decryptedPropertyValue;

	}
}
