package com.fp.common.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.aspose.cells.Cell;
import com.aspose.cells.CellValueType;
import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.aspose.cells.WorksheetCollection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

@SuppressWarnings("restriction")
@Component
public class CommonHTTPListener {

	@Autowired
	private CommonPropertyBean propertyBean;

	private static Logger LOGGER = Logger.getLogger(CommonHTTPListener.class.getName());
	ObjectMapper mapper = new ObjectMapper();

	public void startHttpServer(String contextType) {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(Integer.parseInt(propertyBean.getHttpServerPort())), 0);
			server.createContext("/", new MyHandler());
			server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool()); // creates a default executor
			server.start();

			LOGGER.info("HTTP Server started on port " + propertyBean.getHttpServerPort() + " and application context is '/'");
		}
		catch(Exception e) {
			LOGGER.info("Error occured in starting HTTP Port " + propertyBean.getHttpServerPort());
			e.printStackTrace();
			// Main.sendMail(e);
			// return false;
		}
	}

	class MyHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange httpExchange) throws IOException {

			LOGGER.info("HTTP Context: " + httpExchange.getRequestURI().getPath());
			LOGGER.info("Request Method" + httpExchange.getRequestMethod());
			LOGGER.info("ContentType:" + httpExchange.getRequestHeaders().get("Content-Type"));
			long before = System.currentTimeMillis();
			String response = null;
			String versionId = null;
			String clientId = null;
			RPEngineRequest rpRequest = new RPEngineRequest();
			SIPEngineRequest assetAllocationRequest = new SIPEngineRequest();
			Workbook wb = null;

			if(httpExchange.getRequestMethod().equals("POST") && httpExchange.getRequestHeaders().get("Content-Type").contains("application/json")) {

				String requestJSON = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody())).lines()
						.collect(Collectors.joining("\n"));

				long beforeLoad = System.currentTimeMillis();

				// check the context type.
				if(httpExchange.getRequestURI().getPath().contains(CommonEngineConstants.RP_CONTEXT)) {
					rpRequest = mapper.readValue(requestJSON, RPEngineRequest.class);
					versionId = rpRequest.getVersionId().substring(rpRequest.getVersionId().length() - 2);
					clientId = rpRequest.getVersionId().substring(0, rpRequest.getVersionId().length() - 2);
					wb = WorkbookPool.getWB(CommonEngineConstants.RP_INPUT_WORKBOOK, clientId + "_" + versionId);
				}

				if(httpExchange.getRequestURI().getPath().contains(CommonEngineConstants.AA_CONTEXT)) {
					assetAllocationRequest = mapper.readValue(requestJSON, SIPEngineRequest.class);
					versionId = assetAllocationRequest.getVersionId().substring(assetAllocationRequest.getVersionId().length() - 2);
					clientId = assetAllocationRequest.getVersionId().substring(0, assetAllocationRequest.getVersionId().length() - 2);
					wb = WorkbookPool.getWB(CommonEngineConstants.AA_INPUT_WORKBOOK, clientId + "_" + versionId);
				}

				long wbLoadTime = System.currentTimeMillis() - beforeLoad;
				if(null != wb) {

					// get response
					try {
						response = fetchResponseJSON(httpExchange, wb, rpRequest, assetAllocationRequest);
					}
					catch(Exception e) {
						String s = "Exception thrown while processing";
						httpExchange.sendResponseHeaders(404, s.length());
						httpExchange.getResponseBody().write(s.getBytes());
						httpExchange.getResponseBody().close();
						LOGGER.info("Rejecting HTTP Request recieved from " + httpExchange.getRemoteAddress() + httpExchange.getRequestURI()
								+ " as exception is thrown while reading or working on excel.");
					}

					long sendResStart = System.currentTimeMillis();
					httpExchange.sendResponseHeaders(200, response.length());
					OutputStream os = httpExchange.getResponseBody();
					os.write(response.getBytes());
					os.close();

					addWorkbookToPool(httpExchange, wb, clientId, versionId);

				} else {
					long itr = 0;
					while(null == wb) {
						LOGGER.info(versionId + " Wb Pool exhausted... retrying in 100 milli.");
						try {
							Thread.currentThread().sleep(100);
						}
						catch(InterruptedException e) {}

						if(httpExchange.getRequestURI().getPath().contains(CommonEngineConstants.RP_CONTEXT)) {
							wb = WorkbookPool.getWB(CommonEngineConstants.RP_INPUT_WORKBOOK, clientId + "_" + versionId);
						}

						if(httpExchange.getRequestURI().getPath().contains(CommonEngineConstants.AA_CONTEXT)) {
							wb = WorkbookPool.getWB(CommonEngineConstants.AA_INPUT_WORKBOOK, clientId + "_" + versionId);
						}
						itr++;
					}

					// get response
					try {
						response = fetchResponseJSON(httpExchange, wb, rpRequest, assetAllocationRequest);
					}
					catch(Exception e) {
						String s = "Exception thrown while processing";
						httpExchange.sendResponseHeaders(404, s.length());
						httpExchange.getResponseBody().write(s.getBytes());
						httpExchange.getResponseBody().close();
						LOGGER.info("Rejecting HTTP Request recieved from " + httpExchange.getRemoteAddress() + httpExchange.getRequestURI()
								+ " as exception is thrown while reading or working on excel.");
					}

					long sendResStart = System.currentTimeMillis();
					httpExchange.sendResponseHeaders(200, response.length());
					OutputStream os = httpExchange.getResponseBody();
					os.write(response.getBytes());
					os.close();

					addWorkbookToPool(httpExchange, wb, clientId, versionId);
				}
			} else {
				String s = "Forbidden Access";
				httpExchange.sendResponseHeaders(403, s.length());
				httpExchange.getResponseBody().write(s.getBytes());
				httpExchange.getResponseBody().close();
				LOGGER.info("Rejecting HTTP Request recieved from " + httpExchange.getRemoteAddress() + httpExchange.getRequestURI()
						+ " as either it is not part of supported list or Wrong Application context.");
			}
		}

		private void addWorkbookToPool(HttpExchange httpExchange, Workbook wb, String clientId, String versionId) {

			// return workbook to pool
			long beforeAdd = System.currentTimeMillis();
			// check the context type.
			if(httpExchange.getRequestURI().getPath().contains(CommonEngineConstants.RP_CONTEXT)) {
				WorkbookPool.addWB(CommonEngineConstants.RP_INPUT_WORKBOOK, clientId + "_" + versionId, wb);
			}

			if(httpExchange.getRequestURI().getPath().contains(CommonEngineConstants.AA_CONTEXT)) {
				WorkbookPool.addWB(CommonEngineConstants.AA_INPUT_WORKBOOK, clientId + "_" + versionId, wb);
			}

			long wbAddtime = System.currentTimeMillis() - beforeAdd;
			LOGGER.info(versionId + " Request completed - time taken wbAddtime=" + wbAddtime);

		}

		private String fetchResponseJSON(HttpExchange httpExchange, Workbook wb, RPEngineRequest rpRequest, SIPEngineRequest assetAllocationRequest)
				throws Exception {

			String riskScore = null;
			Map<String, SIPDetails> sipDetails = new HashMap<String, SIPDetails>();
			String response = null;

			// check the context type.
			if(httpExchange.getRequestURI().getPath().contains(CommonEngineConstants.RP_CONTEXT)) {
				riskScore = getRiskScore(wb, rpRequest.getQuestionAnswersList());
				RPEngineResponse engineResponse = new RPEngineResponse();
				engineResponse.setResponseCode(HttpStatus.OK.toString());
				engineResponse.setRiskScore(riskScore);
				response = mapper.writeValueAsString(engineResponse);
			}

			if(httpExchange.getRequestURI().getPath().contains(CommonEngineConstants.AA_CONTEXT)) {
				sipDetails = getSIPCalc(wb, assetAllocationRequest.getGoalDetails(), assetAllocationRequest.getRiskScore());
				SIPEngineResponse engineResponse = new SIPEngineResponse();
				engineResponse.setResponseCode(HttpStatus.OK.toString());
				engineResponse.setSipDetails(sipDetails);
				response = mapper.writeValueAsString(engineResponse);
			}
			return response;
		}

		/**
		 * Method to return riskScore
		 * 
		 * @param workbook
		 * @param quesAnsList
		 * @return riskScore
		 */
		private String getRiskScore(Workbook workbook, List<QuestionAnswersRequest> quesAnsList) {

			String riskScore = null;

			WorksheetCollection sheets = workbook.getWorksheets();
			Worksheet sheet = sheets.get(CommonEngineConstants.RP_SCORING_SHEET);
			Cells cells = sheet.getCells();

			outerLoop: for(QuestionAnswersRequest questionAnswersRequest:quesAnsList) {
				for(int i = 0; i < quesAnsList.size(); i++) {
					int index = 2;
					index = index + i;
					Cell quesCell = cells.get("A" + index);

					switch (quesCell.getType()) {
						case CellValueType.IS_STRING:
							if(quesCell.getStringValue().equals(questionAnswersRequest.getQuestion())) {
								Cell ansCell = cells.get("B" + index);
								ansCell.setValue(questionAnswersRequest.getAnswer());
							}
							break;
						case CellValueType.IS_NULL:
							break outerLoop;
					}
				}
			}

			// Calculating the results of formulas
			workbook.calculateFormula();

			// fetch riskScore
			Cell riskScoreCell = cells.get("F2");

			// Get string value without any formatting
			riskScore = Integer.toString(riskScoreCell.getIntValue());

			LOGGER.info("RiskScore Calculated= " + riskScore);

			return riskScore;
		}
	}

	/**
	 * Method to calculate SIP
	 * 
	 * @param wb
	 * @param goalDetailsList
	 * @param riskVal
	 * @return
	 * @throws Exception
	 */
	public Map<String, SIPDetails> getSIPCalc(Workbook wb, List<GoalDetails> goalDetailsList, Integer riskVal) throws Exception {

		Map<String, SIPDetails> sipDetails = new HashMap<String, SIPDetails>();

		for(GoalDetails goalDetail:goalDetailsList) {

			SIPDetails details = null;

			// check whether goal is recurring goal or not. and change the tenor for each goal.
			if(goalDetail.isRecurringGoal()) {
				Integer tenor = 0;
				for(int i = 0; i < Integer.parseInt(goalDetail.getRecurringGoalFreq()); i++) {
					tenor = tenor + Integer.parseInt(goalDetail.getTenor());
					details = fetchSIPDetailsPerGoal(wb, riskVal, goalDetail, tenor);
					sipDetails.put(goalDetail.getGoalName() + "_" + tenor, details);
				}
			} else {
				details = fetchSIPDetailsPerGoal(wb, riskVal, goalDetail, Integer.parseInt(goalDetail.getTenor()));
				sipDetails.put(goalDetail.getGoalName(), details);
			}

		}

		return sipDetails;
	}

	private SIPDetails fetchSIPDetailsPerGoal(Workbook workbook, Integer riskVal, GoalDetails goalDetail, Integer tenor) throws Exception {

		ArrayList<Double> factorValue = new ArrayList<Double>();
		WorksheetCollection sheets = workbook.getWorksheets();
		Worksheet worksheet = sheets.get(CommonEngineConstants.AA_MATRIX_SHEET);
		LOGGER.info("Sheet Name: " + worksheet.getName());
		double equityExpo = 0.00;
		double sTermExpo = 0.00;
		double lTermExpo = 0.00;
		double gTermExpo = 0.00;
		double rt  = 0.00;
		Cell lastCell = worksheet.getCells().endCellInColumn((short) 0);
		for(int row = 0; row <= lastCell.getRow(); row++) {
			Cell cell = worksheet.getCells().get(row, 0);
			switch (cell.getType()) {
				case CellValueType.IS_NUMERIC:
					if(cell.getIntValue() == riskVal) {
						int matrixRowNumber = row + 1;
						Cell cellName = worksheet.getCells().get("B" + matrixRowNumber);
						if(cellName.getIntValue() == tenor) {
							int factor = 1;
							factorValue = new ArrayList<Double>();
							for(int i = tenor; i > 0; i--) {

								equityExpo = worksheet.getCells().get("C" + matrixRowNumber).getDoubleValue();
								sTermExpo = worksheet.getCells().get("D" + matrixRowNumber).getDoubleValue();
								lTermExpo = worksheet.getCells().get("E" + matrixRowNumber).getDoubleValue();
								
								double[] calReturn = {equityExpo, sTermExpo, lTermExpo};
								double exReturnValue = calculateExpectedReturn(calReturn, sheets);
								if(factor==1){
									rt = exReturnValue;
								}
								double calFactor = calculateFactor(exReturnValue, tenor, factor);
								factorValue.add(calFactor);
								matrixRowNumber++;
								factor++;
							}
						}
					}
					break;
			}
		}
		SIPDetails details = new SIPDetails();
		double inflationValue = calculateInflation(goalDetail, sheets);
		double futureValue = calculateFV(goalDetail, inflationValue, tenor);
		double sipVal = calculateSIP(factorValue, futureValue);
		details.setSipValue(sipVal);
		details.setFinalValue(futureValue);
		details.setInflationValue(inflationValue);
		details.setExpectedReturn(rt);
		return details;
	}

	/**
	 * Calculate SIP Monthly
	 * 
	 * @param factorValue
	 * @param finalValue
	 * @return
	 * @throws Exception
	 */
	private double calculateSIP(ArrayList<Double> factorValue, double finalValue) throws Exception {
		double factorSum = 0.0;
		double sipValueAnually = 0.0;
		for(Double sum:factorValue) {
			factorSum = factorSum + sum;
		}
		sipValueAnually = finalValue / factorSum;

		return (sipValueAnually / 12);
	}

	/**
	 * Calculate Final Value
	 * 
	 * @param goalDetail
	 * @param inflationValue
	 * @param tenor
	 * @return
	 * @throws Exception
	 */
	private double calculateFV(GoalDetails goalDetail, double inflationValue, Integer tenor) throws Exception {
		double finalValue = 0.0;
		finalValue = (Double.parseDouble(goalDetail.getPresentValue())) * Math.pow((1 + inflationValue), tenor.doubleValue());
		return finalValue;
	}

	/**
	 * Calculate Inflation
	 * 
	 * @param goalDetail
	 * @param sheets
	 * @return
	 * @throws Exception
	 */
	private double calculateInflation(GoalDetails goalDetail, WorksheetCollection sheets) throws Exception {

		Worksheet worksheet = sheets.get(CommonEngineConstants.AA_INFLATION_SHEET);
		double inflationValue = 0.0;
		if(worksheet.getCells().get("A2").getValue().equals(goalDetail.getGoalCategory())) {
			inflationValue = (double) worksheet.getCells().get("C2").getValue();
		} else if(worksheet.getCells().get("A3").getValue().equals(goalDetail.getGoalCategory())) {
			inflationValue = (double) worksheet.getCells().get("C3").getValue();
		} else if(worksheet.getCells().get("A4").getValue().equals(goalDetail.getGoalCategory())) {
			inflationValue = (double) worksheet.getCells().get("C4").getValue();
		} else if(worksheet.getCells().get("A5").getValue().equals(goalDetail.getGoalCategory())) {
			inflationValue = (double) worksheet.getCells().get("C5").getValue();
		} else if(worksheet.getCells().get("A6").getValue().equals(goalDetail.getGoalCategory())) {
			inflationValue = (double) worksheet.getCells().get("C6").getValue();
		} else {
			inflationValue = (double) worksheet.getCells().get("C7").getValue();
		}

		return inflationValue;
	}

	/**
	 * Calculate Factor
	 * 
	 * @param exReturnValue
	 * @param tenor
	 * @param factor
	 * @return
	 */
	private double calculateFactor(double exReturnValue, int tenor, int factor) {
		double returnVal = exReturnValue + 1;
		double tenVal = (tenor + 1) - factor;
		double square = Math.pow(returnVal, tenVal);
		return square;
	}

	/**
	 * Calculate Expected Return
	 * 
	 * @param calReturn
	 * @param sheets
	 * @return
	 * @throws Exception
	 */
	private double calculateExpectedReturn(double[] calReturn, WorksheetCollection sheets) throws Exception {
		double[] fixedRetVal = getExpectedReturnFromSheet(sheets);
		double[] productVal = {fixedRetVal[0] * calReturn[0], fixedRetVal[1] * calReturn[1], fixedRetVal[2] * calReturn[2]};
		double sumProduct = productVal[0] + productVal[1] + productVal[2] ;

		return sumProduct;
	}

	/**
	 * Read Expected Return
	 * 
	 * @param sheets
	 * @return
	 * @throws Exception
	 */
	private double[] getExpectedReturnFromSheet(WorksheetCollection sheets) throws Exception {

		Worksheet worksheet = sheets.get(CommonEngineConstants.AA_EXPECTED_RETURN_SHEET);
		double equity = (double) worksheet.getCells().get("B4").getValue();
		double sTermDebt = (double) worksheet.getCells().get("C4").getValue();
		double lTermDebt = (double) worksheet.getCells().get("D4").getValue();
		double[] fixedRetVal = {equity, sTermDebt, lTermDebt};

		return fixedRetVal;
	}

}
