package com.fp.common.engine;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.aspose.cells.License;
import com.aspose.cells.Workbook;

@Component
public class FPCommonEngine {

	@Autowired
	private CommonPropertyBean propertyBean;

	@Autowired
	private CommonHTTPListener httpListener;

	private static Logger LOGGER = Logger.getLogger(FPCommonEngine.class.getName());

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		final ApplicationContext appContext = new ClassPathXmlApplicationContext("classpath*:application-context.xml");
		final FPCommonEngine commonEngine = appContext.getBean(FPCommonEngine.class);

		boolean flag = false;

		try {
			flag = commonEngine.initialize();
		}
		catch(Exception e1) {
			LOGGER.info("Initialization failed... System will exit.");
			System.exit(0);
		}
		if(!flag) {
			LOGGER.info("Initialization failed... System will exit.");
			System.exit(0);
		}

		commonEngine.httpListener.startHttpServer("RiskProfile");

		while(true) {
			for(String key:WorkbookPool.wbPool.keySet()) {
				LOGGER.info("Pool size with key=" + key + "  -  " + WorkbookPool.wbPool.get(key).size());
			}

			// LOGGER.info("Scanning pool size & Health...");
			try {
				Thread.currentThread();
				Thread.sleep(10 * 1000);
			}
			catch(InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// ((ConfigurableApplicationContext) appContext).close();
	}

	private boolean initialize() throws IOException, Exception {
		setAsposeLicense();

		File[] listOfFiles = new File(propertyBean.getScoringPath()).listFiles((new FilenameFilter() {
			@Override
			public boolean accept(File directory, String fileName) {
				return (fileName.endsWith(".xlsx") && (fileName.startsWith(CommonEngineConstants.RP_INPUT_WORKBOOK)
						|| fileName.startsWith(CommonEngineConstants.AA_INPUT_WORKBOOK)));
			}
		}));
		LOGGER.info("count of files for processing is " + listOfFiles.length);
		String version = "";
		for(int i = 0; i < listOfFiles.length; i++) {
			String fileName = listOfFiles[i].getName();
			LOGGER.info(fileName);
			version = extractVersionFromName(fileName);
			List<Workbook> wb = WorkbookPool.generatePoolOfWB(propertyBean.getScoringPath() + "\\" + fileName);
			if(!(wb.size() == WorkbookPool.poolSize)) {
				LOGGER.info("Could not generate Workbook pool.");
				return false;
			}

			// Append File Starting name with version as key
			StringBuilder key = new StringBuilder();
			key.append(fileName.substring(0, fileName.indexOf("_")));
			key.append(version);
			WorkbookPool.wbPool.put(key.toString(), wb);
			LOGGER.info("WbPool created for file = " + listOfFiles[i].getName() + " with version:" + version + " - size " + WorkbookPool.poolSize);
		}
		return true;
	}

	private String extractVersionFromName(String str) {
		String version = str.substring(str.indexOf("_") + 1, str.lastIndexOf("."));
		return version;
	}

	/**
	 * Method to set Aspose License
	 * 
	 * @throws IOException
	 * @throws Exception
	 */
	private void setAsposeLicense() throws IOException, Exception {
		License license = new License();
		license.setLicense(new ClassPathResource("Aspose.Cells.lic").getInputStream());
	}

}
