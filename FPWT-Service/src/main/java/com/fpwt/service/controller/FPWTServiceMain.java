package com.fpwt.service.controller;

import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpwt.service.beans.AssetAllocationRequest;
import com.fpwt.service.beans.AssetAllocationResponse;
import com.fpwt.service.beans.GoalDetails;
import com.fpwt.service.beans.RiskProfileRequest;
import com.fpwt.service.beans.RiskProfileResponse;
import com.fpwt.service.beans.SIPDetails;
import com.fpwt.service.beans.WTRequest;
import com.fpwt.service.beans.WTResponse;
import com.fpwt.wtservice.WTService;

@RestController
public class FPWTServiceMain {

	@Autowired
	private FPServiceProcessor fpServiceProcessor;

	@Autowired
	private WTService wtService;

	 private final Logger LOGGER = LoggerFactory.getLogger(FPWTServiceMain.class.getName());
	 
	ObjectMapper mapper = new ObjectMapper();

	@RequestMapping(value = {"/riskProfile/pdf"}, method = {RequestMethod.POST}, consumes = {"application/json"}, produces = {"application/json"})
	public @ResponseBody RiskProfileResponse createPdf(@RequestBody String jsonInput) {

		RiskProfileResponse response = new RiskProfileResponse();
		try {
			RiskProfileRequest riskProfileRequest = mapper.readValue(jsonInput, RiskProfileRequest.class);
			MDC.put("clientRMID", riskProfileRequest.getRmid());
			Integer riskScore = fpServiceProcessor.getRiskScore(riskProfileRequest);

			String riskLevel = null;
			String base64Value = null;
			if(null != riskScore) {
				LOGGER.info(riskScore.toString());
				if((riskScore.intValue() >= 1) && (riskScore.intValue() < 4)) {
					riskLevel = "Conservative";
				} else if((riskScore.intValue() >= 4) && (riskScore.intValue() < 7)) {
					riskLevel = "Moderate";
				} else if((riskScore.intValue() >= 7) && (riskScore.intValue() <= 10)) {
					riskLevel = "Very Aggressive";
				}
			}
			byte[] pdfByteArray = fpServiceProcessor.getRiskProfillingPDFTemplate(riskScore, riskProfileRequest);
			if(null == pdfByteArray) {
				base64Value = null;
				LOGGER.info("Base64Value is null");
				throw new NullPointerException();
			}
			base64Value = Base64.getEncoder().encodeToString(pdfByteArray);

			response.setLevel(riskLevel);
			response.setScore(riskScore.toString());
			response.setBase64Value(base64Value);
		}
		catch(JsonParseException e) {
			LOGGER.error(e.getMessage());
		}
		catch(JsonMappingException e) {
			LOGGER.error(e.getMessage());
		}
		catch(IOException e) {
			LOGGER.error(e.getMessage());
		}
		catch(Exception e) {
			LOGGER.error(e.getMessage());
		}finally {
			MDC.remove("clientRMID");
		}
		return response;
	}

	/**
	 * @param jsonInput
	 * @return AssetAllocationResponse Generate AssetAllocation PDF SIP
	 * Calculation Engine
	 */
	@RequestMapping(value = {"/assetAllocation/pdf"}, method = {RequestMethod.POST}, consumes = {"application/json"}, produces = {"application/json"})
	public @ResponseBody AssetAllocationResponse createAssetAllocationPdf(@RequestBody String jsonInput)
			throws JsonParseException, JsonMappingException, IOException {

		AssetAllocationResponse assetAllocationResponse = new AssetAllocationResponse();
		LinkedHashMap<String, SIPDetails> sipDetails = new LinkedHashMap<String, SIPDetails>();
		String base64Value = null;
		try {
			AssetAllocationRequest assetAllocationRequest = mapper.readValue(jsonInput, AssetAllocationRequest.class);
			MDC.put("clientRMID", assetAllocationRequest.getRmid());
			// validate request
			if(validateAssetAllocationRequest(assetAllocationRequest)) {

				sipDetails = fpServiceProcessor.calculateSIP(assetAllocationRequest);
				byte[] pdfByteArray = fpServiceProcessor.getAssetPDFTemplate(sipDetails, assetAllocationRequest);
				if(null == pdfByteArray) {
					base64Value = null;
					LOGGER.info("Base64Value is null");
					throw new NullPointerException();
				}
				base64Value = Base64.getEncoder().encodeToString(pdfByteArray);
				assetAllocationResponse.setBase64Value(base64Value);
			} else {
				LOGGER.info("Invalid request. Total count of goals are more than 20 or one of the goal's tenor is more than 30years.");
				assetAllocationResponse
						.setBase64Value("Invalid request. Total count of goals are more than 20 or one of the goal's tenor is more than 30years.");
			}
		}
		catch(JsonParseException e) {
			LOGGER.error(e.getMessage());
		}
		catch(Exception e) {
			LOGGER.error(e.getMessage());
		}finally {
			MDC.remove("clientRMID");
		}

		return assetAllocationResponse;
	}

	/**
	 * Method to validate the asset allocation request
	 * 
	 * @param assetAllocationRequest
	 * @return
	 */
	private boolean validateAssetAllocationRequest(AssetAllocationRequest assetAllocationRequest) {

		boolean isValidRequest = false;
		int totalGoalCount = 0;

		for(int i = 0; i < assetAllocationRequest.getGoalDetails().size(); i++) {
			GoalDetails goalDetails = assetAllocationRequest.getGoalDetails().get(i);

			if(goalDetails.isRecurringGoal()) {
				totalGoalCount = Integer.parseInt(goalDetails.getRecurringGoalFreq());

				// now also check the no. of tenor years is less than 30
				if((Integer.parseInt(goalDetails.getRecurringGoalFreq()) * Integer.parseInt(goalDetails.getTenor())) > 30) {
					break;
				}
			} else {
				if(Integer.parseInt(goalDetails.getTenor()) > 30) {
					break;
				}
				totalGoalCount++;
			}
		}

		if(totalGoalCount <= 20) {
			isValidRequest = true;
		}

		return isValidRequest;
	}

	/**
	 * WT Calculation
	 * 
	 * @param jsonInput
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	@RequestMapping(value = {"/wt/pdf"}, method = {RequestMethod.POST}, consumes = {"application/json"}, produces = {"application/json"})
	public @ResponseBody WTResponse createWTPdf(@RequestBody String jsonInput) {

		WTResponse wtResponse = new WTResponse();
		String base64Value = "";
		try {
			WTRequest wtRequest = mapper.readValue(jsonInput, WTRequest.class);
			MDC.put("clientRMID", wtRequest.getRmid());
			byte[] pdfByteArray = wtService.createWTPdf(wtRequest);
			if(null == pdfByteArray) {
				base64Value = null;
				LOGGER.info("Base64Value is null");
				throw new NullPointerException();
			}
			base64Value = Base64.getEncoder().encodeToString(pdfByteArray);
			wtResponse.setBase64Value(base64Value);
		}
		catch(JsonParseException e) {
			LOGGER.error(e.getMessage());
		}
		catch(JsonMappingException e) {
			LOGGER.error(e.getMessage());
		}
		catch(IOException e) {
			LOGGER.error(e.getMessage());
		}
		catch(Exception e) {
			LOGGER.error(e.getMessage());
		}finally{
			MDC.remove("clientRMID");
		}

		return wtResponse;
	}

}