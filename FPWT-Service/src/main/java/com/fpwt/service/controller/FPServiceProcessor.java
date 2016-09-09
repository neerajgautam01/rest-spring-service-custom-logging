package com.fpwt.service.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.aspose.cells.BorderType;
import com.aspose.cells.Cell;
import com.aspose.cells.CellBorderType;
import com.aspose.cells.CellValueType;
import com.aspose.cells.Cells;
import com.aspose.cells.Chart;
import com.aspose.cells.Color;
import com.aspose.cells.FindOptions;
import com.aspose.cells.License;
import com.aspose.cells.LookAtType;
import com.aspose.cells.Range;
import com.aspose.cells.Row;
import com.aspose.cells.SaveFormat;
import com.aspose.cells.Series;
import com.aspose.cells.SeriesCollection;
import com.aspose.cells.Style;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.aspose.cells.WorksheetCollection;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpwt.service.beans.AssetAllocationRequest;
import com.fpwt.service.beans.GoalDetails;
import com.fpwt.service.beans.ProfileDetails;
import com.fpwt.service.beans.QuestionAnswersRequest;
import com.fpwt.service.beans.RPEngineRequest;
import com.fpwt.service.beans.RPEngineResponse;
import com.fpwt.service.beans.RiskProfileRequest;
import com.fpwt.service.beans.SIPDetails;
import com.fpwt.service.beans.SIPEngineRequest;
import com.fpwt.service.beans.SIPResponseEngine;
import com.fpwt.service.common.FPWTConstants;
import com.fpwt.service.common.FPWTPropertyBean;

@Service
public class FPServiceProcessor {

	@Autowired
	private FPWTPropertyBean propertyBean;

	RestTemplate restTemplate = new RestTemplate();
	ObjectMapper mapper = new ObjectMapper();
	private static Logger LOGGER = LoggerFactory.getLogger(FPServiceProcessor.class.getName());

	public Integer getRiskScore(RiskProfileRequest riskProfileRequest)
			throws JsonParseException, JsonMappingException, IOException {
		LOGGER.info("Connecting to Risk Profiler Engine.");
		Integer riskScore = null;
		RPEngineRequest rpRequest = riskProfileRequest.getRpRequest();

		String inputJsonString = mapper.writeValueAsString(rpRequest);

		String rpEngineURL = "http://" + propertyBean.getHttpServerIP() + ":" + propertyBean.getHttpServerPort()
				+ propertyBean.getHttpServerContext();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(inputJsonString, headers);

		ResponseEntity<String> loginResponse = restTemplate.exchange(rpEngineURL, HttpMethod.POST, entity, String.class,
				new Object[0]);
		if (loginResponse.getStatusCode() == HttpStatus.OK) {
			RPEngineResponse rpResponse = mapper.readValue(loginResponse.getBody(), RPEngineResponse.class);
			if (null != rpResponse) {
				riskScore = Integer.valueOf(Integer.parseInt(rpResponse.getRiskScore()));
			}
		}
		return riskScore;
	}

	/**
	 * Method to return byte[] of final PDF template after inserting all data.
	 * 
	 * @param riskScore
	 * @param riskProfileRequest
	 * @return
	 * @throws Exception
	 */
	public byte[] getRiskProfillingPDFTemplate(Integer riskScore, RiskProfileRequest riskProfileRequest)
			throws Exception {
		setAsposeLicense();

		byte[] pdfBytesArray = null;
		String fileName = null;
		String folderPath = propertyBean.getTemplatePath();
		RPEngineRequest rpRequest = riskProfileRequest.getRpRequest();

		List<String> fileNamesList = listFilesForFolder(new File(folderPath));
		for (String fName : fileNamesList) {
			String[] fileNamesArray = fName.split("_");
			if (fileNamesArray.length == 4) {
				String[] verionArray = fileNamesArray[3].split("\\.");
				if ((fileNamesArray[0].equalsIgnoreCase(FPWTConstants.RISK_PROFILE_WORKBOOK))
						&& (rpRequest.getVersionId().contains(fileNamesArray[2]))
						&& (rpRequest.getVersionId().contains(verionArray[0]))) {
					fileName = fName;
				}
			}
		}
		LOGGER.info("FileName to be processed: " + fileName);
		if (null != fileName) {
			Workbook workbook = new Workbook(folderPath + "\\" + fileName);
			WorksheetCollection sheets = workbook.getWorksheets();
			Worksheet worksheet = sheets.get(FPWTConstants.RISK_PROFILE_SHEET);

			LOGGER.info("Sheet Name: " + worksheet.getName());

			setProfileDetailsInTemplate(riskProfileRequest, worksheet);
			setAnswersInTemplate(rpRequest, worksheet);

			String riskScoreCellIndex = findRow(worksheet, FPWTConstants.RISK_SCORE_CELL_VALUE);
			if (null != riskScoreCellIndex) {
				highLightRiskScore(riskScoreCellIndex, worksheet, riskScore, FPWTConstants.RISK_PROFILE_WORKBOOK);
			}

			sheets.get(FPWTConstants.RISK_SHEET_TOBE_REMOVED).setVisible(false);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			workbook.save(outputStream, SaveFormat.PDF);

			pdfBytesArray = outputStream.toByteArray();
		}
		return pdfBytesArray;
	}

	/**
	 * Method to set profile details in template
	 * 
	 * @param riskProfileRequest
	 * @param worksheet
	 */
	private void setProfileDetailsInTemplate(RiskProfileRequest riskProfileRequest, Worksheet worksheet) {
		ProfileDetails profileDetails = riskProfileRequest.getProfileDetails();
		Integer age = profileDetails.getAge();
		String name = profileDetails.getName();
		String occupation = profileDetails.getOccupation();

		Cells cells = worksheet.getCells();
		for (int i = 3; i <= 5; i++) {
			Cell cell = cells.get("C" + i);
			switch (i) {
			case 3:
				cell.setValue(name);
				break;
			case 4:
				cell.setValue(age);
				break;
			case 5:
				cell.setValue(occupation);
			}
		}
	}

	/**
	 * Method to set answers in template
	 * 
	 * @param rpRequest
	 * @param worksheet
	 */
	private void setAnswersInTemplate(RPEngineRequest rpRequest, Worksheet worksheet) {
		Worksheet wksheet = worksheet.getWorkbook().getWorksheets().get(1);
		int questionRowCount = 2;
		int ansRowCount = 6;
		Cells cells = wksheet.getCells();
		for (QuestionAnswersRequest questionAnswersRequest : rpRequest.getQuestionAnswersList()) {
			Cell questionCell = cells.get("A" + questionRowCount);
			switch (questionCell.getType()) {
			case CellValueType.IS_STRING:
				if (questionAnswersRequest.getQuestion().contains(questionCell.getStringValue())) {
					Cell ansCell = cells.get("L" + ansRowCount);
					ansCell.setValue(questionAnswersRequest.getAnswer());
				}
				break;
			case CellValueType.IS_NUMERIC:
				if (questionAnswersRequest.getQuestion().contains(Integer.toString(questionCell.getIntValue()))) {
					Cell ansCell = cells.get("L" + ansRowCount);
					ansCell.setValue(questionAnswersRequest.getAnswer());
				}
				break;
			case CellValueType.IS_NULL:
				return;
			}
			questionRowCount += 6;
			ansRowCount += 6;
		}
	}

	/**
	 * Method to highlight risk score.
	 * 
	 * @param riskScoreCellIndex
	 * @param worksheet
	 * @param riskScore
	 * @param workbookTemplateType
	 */
	private void highLightRiskScore(String riskScoreCellIndex, Worksheet worksheet, Integer riskScore,
			String workbookTemplateType) {
		int colIndexToBeHighlighted = 0;

		int rsLastColIndex = 0;

		String[] indexArray = riskScoreCellIndex.split("_");

		int rsUpBorderRow = Integer.parseInt(indexArray[1]) + 1;

		int rsRow = Integer.parseInt(indexArray[1]) + 2;

		int rsBottomBorderRow = Integer.parseInt(indexArray[1]) + 3;

		int rsCol = Integer.parseInt(indexArray[0]) + 1;

		Cells cells = worksheet.getCells();
		Cell firstRSRowCell = cells.get(rsRow, rsCol);
		if (firstRSRowCell.getIntValue() == 1) {
			Row firstRSRowObj = cells.checkRow(rsRow);
			Iterator rsRowIterator = firstRSRowObj.iterator();
			whileLoop: while (rsRowIterator.hasNext()) {
				Cell cell = (Cell) rsRowIterator.next();
				switch (cell.getType()) {
				case CellValueType.IS_NUMERIC:
					if (cell.getIntValue() == riskScore.intValue()) {
						colIndexToBeHighlighted = cell.getColumn();
					}
					rsLastColIndex++;
					break;
				case CellValueType.IS_NULL:
					if (cell.getColumn() > 0) {
						break whileLoop;
					}
				}
			}
		}
		if ((colIndexToBeHighlighted > 0) && (rsLastColIndex > 0)) {

			removeCellBorders(rsCol, rsLastColIndex, cells, rsUpBorderRow, workbookTemplateType);
			removeCellBorders(rsCol, rsLastColIndex, cells, rsRow, workbookTemplateType);
			removeCellBorders(rsCol, rsLastColIndex, cells, rsBottomBorderRow, workbookTemplateType);

			Cell rsUpBorderRowCell = cells.get(rsUpBorderRow, colIndexToBeHighlighted);
			Cell rsBottomBorderRowCell = cells.get(rsBottomBorderRow, colIndexToBeHighlighted);

			Range range = cells.createRange(rsUpBorderRowCell.getName(), rsBottomBorderRowCell.getName());
			range.setOutlineBorders(CellBorderType.THICK, Color.getBlack());
		}
	}

	/**
	 * Method to remove cell borders of any previous riskScore i.e. any previous
	 * highlighted cell
	 * 
	 * @param rsCol
	 * @param rsLastColIndex
	 * @param cells
	 * @param rowIndex
	 * @param workbookTemplateType
	 */
	private void removeCellBorders(int rsCol, int rsLastColIndex, Cells cells, int rowIndex,
			String workbookTemplateType) {

		if (workbookTemplateType.equals(FPWTConstants.ASSET_ALLOCATION_WORKBOOK)) {

			for (int j = rsCol + 1; j <= rsLastColIndex - 2; j++) {
				Cell cell = cells.get(rowIndex, j);
				Style style = cell.getStyle();
				style.setBorder(BorderType.BOTTOM_BORDER, CellBorderType.NONE, Color.getBlack());
				style.setBorder(BorderType.TOP_BORDER, CellBorderType.NONE, Color.getBlack());
				style.setBorder(BorderType.LEFT_BORDER, CellBorderType.NONE, Color.getBlack());
				style.setBorder(BorderType.RIGHT_BORDER, CellBorderType.NONE, Color.getBlack());
				cell.setStyle(style);
			}

			// now remove borders for 1st and last cell with different style.
			Cell firstCell = cells.get(rowIndex, rsCol);
			Style firstCellStyle = firstCell.getStyle();
			firstCellStyle.setBorder(BorderType.BOTTOM_BORDER, CellBorderType.NONE, Color.getBlack());
			firstCellStyle.setBorder(BorderType.TOP_BORDER, CellBorderType.NONE, Color.getBlack());
			firstCellStyle.setBorder(BorderType.RIGHT_BORDER, CellBorderType.NONE, Color.getBlack());
			firstCell.setStyle(firstCellStyle);

			Cell lastCell = cells.get(rowIndex, rsLastColIndex - 1);
			Style lastCellStyle = lastCell.getStyle();
			lastCellStyle.setBorder(BorderType.BOTTOM_BORDER, CellBorderType.NONE, Color.getBlack());
			lastCellStyle.setBorder(BorderType.TOP_BORDER, CellBorderType.NONE, Color.getBlack());
			lastCellStyle.setBorder(BorderType.LEFT_BORDER, CellBorderType.NONE, Color.getBlack());
			lastCell.setStyle(lastCellStyle);

		} else {

			for (int j = rsCol; j <= rsLastColIndex; j++) {
				Cell cell = cells.get(rowIndex, j);
				Style style = cell.getStyle();
				style.setBorder(BorderType.BOTTOM_BORDER, CellBorderType.NONE, Color.getBlack());
				style.setBorder(BorderType.TOP_BORDER, CellBorderType.NONE, Color.getBlack());
				style.setBorder(BorderType.LEFT_BORDER, CellBorderType.NONE, Color.getBlack());
				style.setBorder(BorderType.RIGHT_BORDER, CellBorderType.NONE, Color.getBlack());
				cell.setStyle(style);
			}
		}

	}

	/**
	 * Method to find the riskScore row.
	 * 
	 * @param worksheet
	 * @param cellContent
	 * @return String "columnIndex_rowIndex"
	 */
	private String findRow(Worksheet worksheet, String cellContent) {

		String cellIndex = null;
		Cells cells = worksheet.getCells();
		FindOptions findOptions = new FindOptions();
		findOptions.setLookAtType(LookAtType.ENTIRE_CONTENT);
		Cell cell = cells.find(cellContent, null, findOptions);
		cellIndex = cell.getColumn() + "_" + cell.getRow();

		return cellIndex;
	}

	/**
	 * Method to list all files of the given folder
	 * 
	 * @param folder
	 * @return
	 */
	private List<String> listFilesForFolder(File folder) {

		List<String> fileNamesList = new ArrayList<String>();
		for (File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				LOGGER.info("Directory is found- verify whether it is output file or not.");
			} else {
				LOGGER.info(fileEntry.getName());
				fileNamesList.add(fileEntry.getName());
			}
		}
		return fileNamesList;
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

	/**
	 * Method to calculate SIP
	 * 
	 * @throws IOException
	 * @throws Exception
	 */
	public LinkedHashMap<String, SIPDetails> calculateSIP(AssetAllocationRequest assetAllocationRequest)
			throws JsonParseException, JsonMappingException, IOException {

		LOGGER.info("Connecting to SIP Calc. Engine");
		LinkedHashMap<String, SIPDetails> sipDetails = new LinkedHashMap<String, SIPDetails>();

		SIPEngineRequest engineRequest = new SIPEngineRequest();
		engineRequest.setRiskScore(assetAllocationRequest.getRiskScore());
		engineRequest.setGoalDetails(assetAllocationRequest.getGoalDetails());
		engineRequest.setVersionId(assetAllocationRequest.getVersionId());

		String inputJsonString = mapper.writeValueAsString(engineRequest);
		String rpEngineURL = "http://" + propertyBean.getHttpServerIP() + ":" + propertyBean.getHttpServerPort()
				+ propertyBean.getHttpAssetServerContext();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(inputJsonString, headers);

		ResponseEntity<String> loginResponse = restTemplate.exchange(rpEngineURL, HttpMethod.POST, entity, String.class,
				new Object[0]);
		if (loginResponse.getStatusCode() == HttpStatus.OK) {
			SIPResponseEngine rpResponse = mapper.readValue(loginResponse.getBody(), SIPResponseEngine.class);
			if (null != rpResponse) {
				sipDetails = (LinkedHashMap<String, SIPDetails>) rpResponse.getSipDetails();
			}
		}

		return sipDetails;
	}

	/**
	 * Method to set asset allocation details in template
	 * 
	 * @param sipDetails
	 * @param assetAllocationRequest
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public byte[] getAssetPDFTemplate(HashMap<String, SIPDetails> sipDetails,
			AssetAllocationRequest assetAllocationRequest) throws IOException, Exception {

		byte[] pdfBytesArray = null;

		setAsposeLicense();
		String fileName = null;
		String folderPath = propertyBean.getTemplatePath();

		List<String> fileNamesList = listFilesForFolder(new File(folderPath));
		for (String fName : fileNamesList) {
			String[] fileNamesArray = fName.split("_");
			if (fileNamesArray.length == 4) {
				String[] verionArray = fileNamesArray[3].split("\\.");
				if ((fileNamesArray[0].equalsIgnoreCase(FPWTConstants.ASSET_ALLOCATION_WORKBOOK))
						&& (assetAllocationRequest.getVersionId().contains(fileNamesArray[2]))
						&& (assetAllocationRequest.getVersionId().contains(verionArray[0]))) {
					fileName = fName;
				}
			}
		}
		LOGGER.info("FileName to be processed: " + fileName);

		if (null != fileName) {
			Workbook workbook = new Workbook(folderPath + "\\" + fileName);
			WorksheetCollection sheets = workbook.getWorksheets();
			Worksheet worksheet = sheets.get(FPWTConstants.ASSET_ALLOCATION_SHEET);
			LOGGER.info("Sheet Name: " + worksheet.getName());

			worksheet.getCells().get("D3").putValue(assetAllocationRequest.getProfileDetails().getName());
			worksheet.getCells().get("D4").putValue(assetAllocationRequest.getProfileDetails().getAge());
			worksheet.getCells().get("D5").putValue(assetAllocationRequest.getProfileDetails().getOccupation());
			String riskScoreCellIndex = findRow(worksheet, FPWTConstants.RISK_SCORE_CELL_VALUE);
			if (null != riskScoreCellIndex) {
				highLightRiskScore(riskScoreCellIndex, worksheet, assetAllocationRequest.getRiskScore(),
						FPWTConstants.ASSET_ALLOCATION_WORKBOOK);
			}
			List<GoalDetails> goalDetailsList = assetAllocationRequest.getGoalDetails();
			int sheetCount = 1;
			for (GoalDetails goalDetail : goalDetailsList) {

				// check whether goal is recurring goal or not. and change the
				// tenor for each goal.
				if (goalDetail.isRecurringGoal()) {
					Integer tenor = 0;

					if (sheetCount == 1) {
						tenor = tenor + Integer.parseInt(goalDetail.getTenor());
						worksheet = sheets.get(FPWTConstants.GOAL_SHEET_1);
						calculateGoalDetails(worksheet, goalDetail, assetAllocationRequest, sipDetails,
								Integer.parseInt(goalDetail.getTenor()));

						for (int recurringFreq = 1; recurringFreq < Integer
								.parseInt(goalDetail.getRecurringGoalFreq()); recurringFreq++) {
							sheetCount++;
							tenor = tenor + Integer.parseInt(goalDetail.getTenor());
							int newSheet = sheets.add();
							worksheet = sheets.get(newSheet);
							worksheet.setName(FPWTConstants.GOAL_SHEET_PREFIX + sheetCount);
							workbook.getWorksheets().get(FPWTConstants.GOAL_SHEET_PREFIX + sheetCount)
									.copy(workbook.getWorksheets().get(FPWTConstants.GOAL_SHEET_1));
							calculateGoalDetails(worksheet, goalDetail, assetAllocationRequest, sipDetails, tenor);
						}
					} else {
						for (int recurringFreq = 0; recurringFreq < Integer
								.parseInt(goalDetail.getRecurringGoalFreq()); recurringFreq++) {
							tenor = tenor + Integer.parseInt(goalDetail.getTenor());
							int newSheet = sheets.add();
							worksheet = sheets.get(newSheet);
							worksheet.setName(FPWTConstants.GOAL_SHEET_PREFIX + sheetCount);
							workbook.getWorksheets().get(FPWTConstants.GOAL_SHEET_PREFIX + sheetCount)
									.copy(workbook.getWorksheets().get(FPWTConstants.GOAL_SHEET_1));
							calculateGoalDetails(worksheet, goalDetail, assetAllocationRequest, sipDetails, tenor);
							sheetCount++;
						}
					}

				} else {

					if (sheetCount == 1) {
						worksheet = sheets.get(FPWTConstants.GOAL_SHEET_1);
						calculateGoalDetails(worksheet, goalDetail, assetAllocationRequest, sipDetails,
								Integer.parseInt(goalDetail.getTenor()));
					} else {
						int newSheet = sheets.add();
						worksheet = sheets.get(newSheet);
						worksheet.setName(FPWTConstants.GOAL_SHEET_PREFIX + sheetCount);
						workbook.getWorksheets().get(FPWTConstants.GOAL_SHEET_PREFIX + sheetCount)
								.copy(workbook.getWorksheets().get(FPWTConstants.GOAL_SHEET_1));
						calculateGoalDetails(worksheet, goalDetail, assetAllocationRequest, sipDetails,
								Integer.parseInt(goalDetail.getTenor()));
					}
				}

				sheetCount++;

			}

			// update image on main page
			calculateImageData(sheets, assetAllocationRequest.getRiskScore());

			// hide unwanted sheets
			hideSheets(sheets);

			// generate pdf byte array.
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			workbook.save(outputStream, SaveFormat.PDF);
			pdfBytesArray = outputStream.toByteArray();

		}

		return pdfBytesArray;
	}

	private void hideSheets(WorksheetCollection sheets) {
		for (int i = 0; i <= 9; i++) {
			sheets.get(FPWTConstants.WT_RISK_SHEETS[i]).setVisible(false);
		}

		sheets.get(FPWTConstants.SHEET_TO_BE_REMOVED).setVisible(false);
		sheets.get(FPWTConstants.ASSET_ALLOCATION_PROFILE_WISE).setVisible(false);

	}

	/**
	 * @param sheets
	 * @param riskScore
	 * @throws Exception
	 */
	private void calculateImageData(WorksheetCollection sheets, Integer riskScore) throws Exception {
		Range defaultRange = getDefaultAssetRange(sheets.get(FPWTConstants.ASSET_ALLOCATION_SHEET));
		if (riskScore == 1) {
			Range riskScoreOneRange = getRiskSheet1Range(sheets.get(FPWTConstants.WT_RISK_SHEETS[0]));
			defaultRange.copy(riskScoreOneRange);
		} else if (riskScore == 2) {
			Range riskScoreTwoRange = getRiskSheet2Range(sheets.get(FPWTConstants.WT_RISK_SHEETS[1]));
			defaultRange.copy(riskScoreTwoRange);
		} else if (riskScore == 3) {
			Range riskScoreThreeRange = getRiskSheet3Range(sheets.get(FPWTConstants.WT_RISK_SHEETS[2]));
			defaultRange.copy(riskScoreThreeRange);
		} else if (riskScore == 4) {
			Range riskScoreFourRange = getRiskSheet4Range(sheets.get(FPWTConstants.WT_RISK_SHEETS[3]));
			defaultRange.copy(riskScoreFourRange);
		} else if (riskScore == 5) {
			Range riskScoreFiveRange = getRiskSheet5Range(sheets.get(FPWTConstants.WT_RISK_SHEETS[4]));
			defaultRange.copy(riskScoreFiveRange);
		} else if (riskScore == 6) {
			Range riskScoreSixRange = getRiskSheet6Range(sheets.get(FPWTConstants.WT_RISK_SHEETS[5]));
			defaultRange.copy(riskScoreSixRange);
		} else if (riskScore == 7) {
			Range riskScoreSevenRange = getRiskSheet7Range(sheets.get(FPWTConstants.WT_RISK_SHEETS[6]));
			defaultRange.copy(riskScoreSevenRange);
		} else if (riskScore == 8) {
			Range riskScoreEightRange = getRiskSheet8Range(sheets.get(FPWTConstants.WT_RISK_SHEETS[7]));
			defaultRange.copy(riskScoreEightRange);
		} else if (riskScore == 9) {
			Range riskScoreNineRange = getRiskSheet9Range(sheets.get(FPWTConstants.WT_RISK_SHEETS[8]));
			defaultRange.copy(riskScoreNineRange);
		} else if (riskScore == 10) {
			Range riskScoreTenRange = getRiskSheet10Range(sheets.get(FPWTConstants.WT_RISK_SHEETS[9]));
			defaultRange.copy(riskScoreTenRange);
		}
	}

	/**
	 * Method to set data for each goals
	 * 
	 * @param goalSheet
	 * @param goalDetail
	 * @param assetAllocationRequest
	 * @param sipDetails
	 * @param tenor
	 * @throws Exception
	 */
	private void calculateGoalDetails(Worksheet goalSheet, GoalDetails goalDetail,
			AssetAllocationRequest assetAllocationRequest, HashMap<String, SIPDetails> sipDetails, Integer tenor)
			throws Exception {
		goalSheet.getCells().get("B2").putValue(goalSheet.getName());
		goalSheet.getCells().get("F3").putValue(goalDetail.getGoalName());
		goalSheet.getCells().get("F4").putValue(Integer.toString(tenor));
		goalSheet.getCells().get("F5").putValue(goalDetail.getPresentValue());
		//goalSheet.getCells().get("F8").putValue("");
		

		Object[] assetMatrixData = getMatrixData(assetAllocationRequest.getRiskScore(), tenor, goalSheet);
		goalSheet.getCells().get("E32").putValue(assetMatrixData[0]);
		goalSheet.getCells().get("H32").putValue(assetMatrixData[1]);
		goalSheet.getCells().get("K32").putValue(assetMatrixData[2]);
		// goalSheet.getCells().get("L32").putValue(assetMatrixData[3]);

		sipDetails.forEach((key, value) -> {

			// recurring goal check.
			if (key.contains("_") && (goalDetail.getGoalName() + "_" + tenor).equalsIgnoreCase(key)) {
				setGoalDetailValues(sipDetails, goalSheet, assetMatrixData, value);

			}

			if (!(key.contains("_")) && goalDetail.getGoalName().equalsIgnoreCase(key)) {
				setGoalDetailValues(sipDetails, goalSheet, assetMatrixData, value);
			}

		});

		// Chart Generation
		Chart chart = goalSheet.getCharts().get(0);
		SeriesCollection seriesCollection = chart.getNSeries();
		for (int i = 0; i < seriesCollection.getCount(); i++) {
			Series series = seriesCollection.get(i);
			if (series.getName().equals("Equity")) {
				series.setValues("='" + FPWTConstants.SHEET_TO_BE_REMOVED + "'!" + assetMatrixData[4]);
			}
			if (series.getName().equals("Debt - Long")) {
				series.setValues("='" + FPWTConstants.SHEET_TO_BE_REMOVED + "'!" + assetMatrixData[5]);
			}
			if (series.getName().equals("Debt - Short")) {
				series.setValues("='" + FPWTConstants.SHEET_TO_BE_REMOVED + "'!" + assetMatrixData[6]);
			}
			/*if (series.getName().equals("Gold")) {
				series.setValues("='" + FPWTConstants.SHEET_TO_BE_REMOVED + "'!" + assetMatrixData[7]);
			}*/
		}

	}

	/**
	 * Method to set goal details except for chart
	 * 
	 * @param sipDetails
	 * @param goalSheet
	 * @param assetMatrixData
	 * @param value
	 */
	private void setGoalDetailValues(HashMap<String, SIPDetails> sipDetails, Worksheet goalSheet,
			Object[] assetMatrixData, SIPDetails value) {

		SIPDetails details = value;
		goalSheet.getCells().get("F6").putValue(details.getFinalValue());
		goalSheet.getCells().get("F7").putValue(details.getInflationValue());
		goalSheet.getCells().get("H11").putValue(details.getSipValue());

		goalSheet.getCells().get("E33")
				.putValue((Double.parseDouble(assetMatrixData[0].toString())) * details.getSipValue());
		goalSheet.getCells().get("H33")
				.putValue((Double.parseDouble(assetMatrixData[1].toString())) * details.getSipValue());
		goalSheet.getCells().get("K33")
				.putValue((Double.parseDouble(assetMatrixData[2].toString())) * details.getSipValue());
		// goalSheet.getCells().get("L33").putValue((Double.parseDouble(assetMatrixData[3].toString()))
		// * details.getSipValue());
		goalSheet.getCells().get("F8").putValue(details.getExpectedReturn());

	}

	/**
	 * Method to fetch the hidden excel data as Object[]
	 * 
	 * @param riskScore
	 * @param tenor
	 * @param goalSheet
	 * @return
	 * @throws Exception
	 */
	private Object[] getMatrixData(Integer riskScore, Integer tenor, Worksheet goalSheet) throws Exception {
		double equityExpo = 0.00;
		double sTermExpo = 0.00;
		double lTermExpo = 0.00;
		double gTermExpo = 0.00;
		String chartEquitySeries = "";
		String chartDebtLongSeries = "";
		String chartDebtShortSeries = "";
		String chartGoldSeries = "";
		Worksheet worksheet = goalSheet.getWorkbook().getWorksheets().get(FPWTConstants.SHEET_TO_BE_REMOVED);

		Cell lastCell = worksheet.getCells().endCellInColumn((short) 0);
		rowLoop: for (int row = 0; row <= lastCell.getRow(); row++) {
			Cell cell = worksheet.getCells().get(row, 0);

			switch (cell.getType()) {
			case CellValueType.IS_NUMERIC:

				if (cell.getIntValue() == riskScore) {
					int matrixRowNumber = row + 1;
					Cell cellName = worksheet.getCells().get("B" + matrixRowNumber);
					if (cellName.getIntValue() == tenor) {

						equityExpo = worksheet.getCells().get("C" + matrixRowNumber).getDoubleValue();
						sTermExpo = worksheet.getCells().get("D" + matrixRowNumber).getDoubleValue();
						lTermExpo = worksheet.getCells().get("E" + matrixRowNumber).getDoubleValue();
						// gTermExpo = worksheet.getCells().get("F" +
						// matrixRowNumber).getDoubleValue();

						// chart related data
						chartEquitySeries = "$C$" + matrixRowNumber + ":$C$" + ((matrixRowNumber + tenor) - 1);
						chartDebtLongSeries = "$D$" + matrixRowNumber + ":$D$" + ((matrixRowNumber + tenor) - 1);
						chartDebtShortSeries = "$E$" + matrixRowNumber + ":$E$" + ((matrixRowNumber + tenor) - 1);
						// chartGoldSeries = "$F$" + matrixRowNumber + ":$F$" +
						// ((matrixRowNumber + tenor) - 1);
						break rowLoop;
					}
				}
				break;
			}

		}
		Object[] calReturn = { equityExpo, sTermExpo, lTermExpo, gTermExpo, chartEquitySeries, chartDebtLongSeries,
				chartDebtShortSeries, chartGoldSeries };
		return calReturn;
	}

	private Range getDefaultAssetRange(Worksheet worksheet) {
		Cells cells = worksheet.getCells();
		Range range = cells.createRange("F13", "M18");

		return range;
	}

	private Range getRiskSheet1Range(Worksheet worksheet) throws Exception {

		//ShapeCollection shapes = worksheet.getShapes();
		//worksheet2.getShapes().addCopy(shapes.get(1), 15, 0, 6, 0);
		Cells cells = worksheet.getCells();
		Range range = cells.createRange("A2", "H7");

		return range;
	}

	private Range getRiskSheet2Range(Worksheet worksheet) {
		Cells cells = worksheet.getCells();
		Range range = cells.createRange("A2", "H7");

		return range;
	}

	private Range getRiskSheet3Range(Worksheet worksheet) {
		Cells cells = worksheet.getCells();
		Range range = cells.createRange("A2", "H7");

		return range;
	}

	private Range getRiskSheet4Range(Worksheet worksheet) {
		Cells cells = worksheet.getCells();
		Range range = cells.createRange("A2", "H7");

		return range;
	}

	private Range getRiskSheet5Range(Worksheet worksheet) {
		Cells cells = worksheet.getCells();
		Range range = cells.createRange("A2", "H7");

		return range;
	}

	private Range getRiskSheet6Range(Worksheet worksheet) {
		Cells cells = worksheet.getCells();
		Range range = cells.createRange("A2", "H7");

		return range;
	}

	private Range getRiskSheet7Range(Worksheet worksheet) {
		Cells cells = worksheet.getCells();
		Range range = cells.createRange("A2", "H7");

		return range;
	}

	private Range getRiskSheet8Range(Worksheet worksheet) {
		Cells cells = worksheet.getCells();
		Range range = cells.createRange("A2", "H7");

		return range;
	}

	private Range getRiskSheet9Range(Worksheet worksheet) {
		Cells cells = worksheet.getCells();
		Range range = cells.createRange("A2", "H7");

		return range;
	}

	private Range getRiskSheet10Range(Worksheet worksheet) {
		Cells cells = worksheet.getCells();
		Range range = cells.createRange("A2", "H7");

		return range;
	}

}