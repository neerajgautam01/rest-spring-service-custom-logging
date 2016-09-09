/**
 * 
 */
package com.fpwt.wtserviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.aspose.cells.Cells;
import com.aspose.cells.Chart;
import com.aspose.cells.ChartPoint;
import com.aspose.cells.ChartPointCollection;
import com.aspose.cells.Color;
import com.aspose.cells.DataLabels;
import com.aspose.cells.LabelPositionType;
import com.aspose.cells.License;
import com.aspose.cells.Range;
import com.aspose.cells.SaveFormat;
import com.aspose.cells.Series;
import com.aspose.cells.SeriesCollection;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.aspose.cells.WorksheetCollection;
import com.fpwt.service.beans.AssetFolioData;
import com.fpwt.service.beans.PerformanceBean;
import com.fpwt.service.beans.WTRequest;
import com.fpwt.service.common.FPWTConstants;
import com.fpwt.service.common.FPWTPropertyBean;
import com.fpwt.service.common.Utility;
import com.fpwt.wtdao.WTDao;
import com.fpwt.wtservice.WTService;

/**
 * @author c-NikunjP
 *
 */
@Service
public class WTServiceImpl implements WTService {

	@Autowired
	private FPWTPropertyBean propertyBean;

	@Autowired
	private WTDao wtDao;
	private final Logger LOGGER = LoggerFactory.getLogger(WTServiceImpl.class.getName());

	/**
	 * Create WT PDF
	 * 
	 * @param wtRequest
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	@Override
	public byte[] createWTPdf(WTRequest wtRequest) throws IOException, Exception {
		setAsposeLicense();

		byte[] pdfBytesArray = null;
		String fileName = null;
		String folderPath = propertyBean.getTemplatePath();
		List<String> fileNamesList = listFilesForFolder(new File(folderPath));
		for(String fName:fileNamesList) {
			String[] fileNamesArray = fName.split("_");
			if((fileNamesArray[0].equalsIgnoreCase("WT"))) {
				fileName = fName;
			}
		}
		LOGGER.info("FileName to be processed: " + fileName);

		if(null != fileName) {
			Workbook workbook = new Workbook(folderPath + "\\" + fileName);
			WorksheetCollection sheets = workbook.getWorksheets();
			Worksheet worksheet = sheets.get(FPWTConstants.WT_TEMPLATE);
			LOGGER.info("Sheet Name: " + worksheet.getName());
			Cells cells = worksheet.getCells();
			cells.removeFormulas();
			Date minDebtDate;

			// Investor Details
			getInvestorData(worksheet, wtRequest);

			// Fund Pulse
			int rowID = fundPulseCalculation(worksheet, wtRequest);

			// Performance Report
			rowID = calculatePerformance(wtRequest.getDetails().getListFolioDtls(), rowID, worksheet);

			// Scheme Code for Equity and Debt and Total
			String[] data = calculateScheduleCode(wtRequest.getDetails().getListFolioDtls());

			if(!data[1].equals("")) {
				minDebtDate = wtDao.getMinDateForDebt(data[1]);
				worksheet.getCells().get("G5").putValue(Utility.formattedDate(minDebtDate));

			} else {
				minDebtDate = null;
				worksheet.getCells().get("G5").putValue("");
			}

			if(minDebtDate != null && !data[2].equals("")) {
				String schemes = wtDao.getAvailableDebtSchemes(Utility.formattedDate(minDebtDate), data[2]);
				LinkedHashMap<String, Double> pieData = wtDao.getSecurityClassForPie(schemes, Utility.formattedDate(minDebtDate),
						data[2].split(",").length);
				// PortFolio Health Check-up Report
				rowID = calculatePieReport(pieData, worksheet, rowID);
			} else {
				// code for delete pie data chart
			}

			// Equity top 10 and top 5 company (Database Connectivity)
			if(!data[0].equals("")) {
				Date minEquityDate = wtDao.getMinDateForEquity(data[0]);
				LinkedHashMap<String, LinkedHashMap<String, Double>> equityData = wtDao.calculateEquityData(data[0], minEquityDate);
				rowID = generatEquityChart(equityData, worksheet, minEquityDate, rowID);
			} else {
				worksheet.getCells().get("E" + rowID).putValue("");
				worksheet.getCells().deleteRows(rowID, 16);
				rowID = rowID + 4;
			}

			// Debt calculation for key/security/asset (Database Connectivity)
			if(!data[1].equals("")) {
				LinkedHashMap<String, Object> debtData = wtDao.calculateDebtData(data[1], minDebtDate);
				rowID = generateDebtChart(debtData, worksheet, rowID, wtRequest.getDetails().getListFolioDtls(), minDebtDate);
			} else {
				if(data[0].equals("")) {
					worksheet.getCells().deleteRows(rowID, 16);
				} else {
					worksheet.getCells().deleteRows(rowID - 4, 15);
				}
			}

			// Contribution Analysis
			generateContAnalysisChart(wtRequest.getCost_value(), wtRequest.getDetails().getListFolioDtls(), worksheet, rowID);

			workbook.getWorksheets().get(1).setVisible(false);
			workbook.getWorksheets().get(2).setVisible(false);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			workbook.save(outputStream, SaveFormat.PDF);
			pdfBytesArray = outputStream.toByteArray();
		}

		return pdfBytesArray;
	}

	private void getInvestorData(Worksheet worksheet, WTRequest wtRequest) {
		worksheet.getCells().get("E4").putValue(wtRequest.getInvestorName());
		worksheet.getCells().get("E5").putValue(wtRequest.getFolioNo());
		worksheet.getCells().get("G4").putValue(wtRequest.getAsOnDate());
	}

	/**
	 * @param pieData
	 * @param worksheet
	 * @param rowID
	 * @return
	 */
	private int calculatePieReport(LinkedHashMap<String, Double> pieData, Worksheet worksheet, int rowID) {
		Worksheet newWorksheet = worksheet.getWorkbook().getWorksheets().add(FPWTConstants.WT_HIDDEN_SHEETS[2]);
		Chart chart = worksheet.getCharts().get(0);
		chart.getNSeries().clear();
		int inputRow = 1;
		if(pieData.size() > 0) {
			for(Map.Entry<String, Double> data:pieData.entrySet()) {
				newWorksheet.getWorkbook().getWorksheets().get(FPWTConstants.WT_HIDDEN_SHEETS[2]).getCells().get("A" + inputRow)
						.putValue(data.getKey());
				newWorksheet.getWorkbook().getWorksheets().get(FPWTConstants.WT_HIDDEN_SHEETS[2]).getCells().get("B" + inputRow)
						.putValue(roundValue(data.getValue()));
				inputRow++;
			}

			chart.getNSeries().add(FPWTConstants.WT_HIDDEN_SHEETS[2] + "!B1:B" + (inputRow - 1), true);
			chart.getNSeries().setCategoryData(FPWTConstants.WT_HIDDEN_SHEETS[2] + "!A1:A" + (inputRow - 1));

			SeriesCollection serieses = chart.getNSeries();

			for(int i = 0; i < serieses.getCount(); i++) {
				Series series = serieses.get(i);
				ChartPointCollection chartPoints = series.getPoints();
				for(int j = 0; j < chartPoints.getCount(); j++) {
					ChartPoint chartPoint = chartPoints.get(j);
					if(j == 0) {
						chartPoint.getArea().setForegroundColor(Color.fromArgb(163, 0, 0));
					} else if(j == 1) {
						chartPoint.getArea().setForegroundColor(Color.fromArgb(255, 64, 0));
					} else if(j == 2) {
						chartPoint.getArea().setForegroundColor(Color.fromArgb(255, 255, 255));
					} else if(j == 3) {
						chartPoint.getArea().setForegroundColor(Color.fromArgb(0, 61, 173));
					} else if(j == 4) {
						chartPoint.getArea().setForegroundColor(Color.fromArgb(188, 211, 255));
					} else if(j == 5) {
						chartPoint.getArea().setForegroundColor(Color.fromArgb(53, 124, 255));
					} else if(j == 6) {
						chartPoint.getArea().setForegroundColor(Color.fromArgb(255, 217, 204));
					} else if(j == 7) {
						chartPoint.getArea().setForegroundColor(Color.fromArgb(255, 116, 116));
					} else if(j == 8) {
						chartPoint.getArea().setForegroundColor(Color.fromArgb(177, 177, 132));
					} else if(j == 9) {
						chartPoint.getArea().setForegroundColor(Color.fromArgb(126, 126, 80));
					} else if(j == 10) {
						chartPoint.getArea().setForegroundColor(Color.fromArgb(191, 48, 0));
					} else if(j == 11) {
						chartPoint.getArea().setForegroundColor(Color.fromArgb(255, 140, 102));
					}
				}

			}

			chart.setShowLegend(true);
			DataLabels dataLabels = chart.getNSeries().get(0).getDataLabels();
			dataLabels.setShowValue(true);
			newWorksheet.getWorkbook().getWorksheets().get(FPWTConstants.WT_HIDDEN_SHEETS[2]).setVisible(false);
		}

		return rowID + 10;
	}

	/**
	 * Calculate Performance
	 * 
	 * @param listFolioDtls
	 * @param rowID
	 * @param worksheet
	 * @return
	 * @throws Exception
	 */
	private int calculatePerformance(ArrayList<AssetFolioData> listFolioDtls, int rowID, Worksheet worksheet) throws Exception {
		String inputData = "";

		Cells cells = worksheet.getCells();
		Range defaultRange = cells.createRange("D" + rowID, "M" + (rowID + 1));
		for(AssetFolioData assetFolioData:listFolioDtls) {
			if(inputData.equals("")) {
				inputData = "P" + assetFolioData.getSchcode() + "";
			} else {
				inputData = inputData + ",P" + assetFolioData.getSchcode() + "";
			}
		}

		if(!inputData.equals("")) {
			ArrayList<PerformanceBean> beans = wtDao.getPerformanceReport(inputData);

			createPerformanceTemplate(beans.size(), worksheet, rowID + 2, defaultRange);

			int i = 1;
			for(PerformanceBean performanceBean:beans) {
				if(i <= 2) {
					if(i % 2 == 0) {
						worksheet.getCells().get("D" + rowID).putValue(performanceBean.getIndexName());
					} else {
						worksheet.getCells().get("D" + rowID).putValue(getSchemeName(performanceBean.getRtaCode(), listFolioDtls));
					}
					worksheet.getCells().get("I" + rowID).putValue(roundValue(performanceBean.getThreeMonthReturn()));
					worksheet.getCells().get("J" + rowID).putValue(roundValue(performanceBean.getSixMonthReturn()));
					worksheet.getCells().get("K" + rowID).putValue(roundValue(performanceBean.getOneYearReturn()));
					worksheet.getCells().get("L" + rowID).putValue(roundValue(performanceBean.getThreeYearReturn()));
					worksheet.getCells().get("M" + rowID).putValue(roundValue(performanceBean.getFiveYearReturn()));
				} else {
					if(i % 2 == 0) {
						worksheet.getCells().get("D" + rowID).putValue(performanceBean.getIndexName());
					} else {
						worksheet.getCells().get("D" + rowID).putValue(getSchemeName(performanceBean.getRtaCode(), listFolioDtls));
					}
					worksheet.getCells().get("I" + rowID).putValue(roundValue(performanceBean.getThreeMonthReturn()));
					worksheet.getCells().get("J" + rowID).putValue(roundValue(performanceBean.getSixMonthReturn()));
					worksheet.getCells().get("K" + rowID).putValue(roundValue(performanceBean.getOneYearReturn()));
					worksheet.getCells().get("L" + rowID).putValue(roundValue(performanceBean.getThreeYearReturn()));
					worksheet.getCells().get("M" + rowID).putValue(roundValue(performanceBean.getFiveYearReturn()));

				}
				rowID++;
				i++;
			}
		}

		return rowID + 4;

	}

	private void createPerformanceTemplate(int size, Worksheet worksheet, int rowID, Range defaultRange) throws Exception {
		Cells cells = worksheet.getCells();
		for(int j = 1; j < size / 2; j++) {
			worksheet.getCells().insertRow(rowID - 1);
			worksheet.getCells().insertRow(rowID);
			Range range2 = cells.createRange("D" + (rowID), "M" + (rowID + 1));
			range2.copy(defaultRange);
		}

	}

	private void generateContAnalysisChart(String totalCostValue, ArrayList<AssetFolioData> listFolioDtls, Worksheet worksheet, int rowID) {
		LOGGER.info("Contribution Analysis Calculation");
		Chart chart = worksheet.getCharts().get(4);
		chart.getNSeries().clear();
		if(listFolioDtls.size() > 0) {
			int inputRow = 1;
			Worksheet newWorksheet = worksheet.getWorkbook().getWorksheets().add(FPWTConstants.WT_HIDDEN_SHEETS[6]);
			for(AssetFolioData assetFolioData:listFolioDtls) {
				newWorksheet.getWorkbook().getWorksheets().get(FPWTConstants.WT_HIDDEN_SHEETS[6]).getCells().get("A" + inputRow)
						.putValue(assetFolioData.getSch_name());
				newWorksheet.getWorkbook().getWorksheets().get(FPWTConstants.WT_HIDDEN_SHEETS[6]).getCells().get("B" + inputRow).putValue(
						roundValue((assetFolioData.getCost_value() * percentageCal(assetFolioData.getXirr())) / Double.parseDouble(totalCostValue)));
				inputRow++;
			}

			chart.getNSeries().add(FPWTConstants.WT_HIDDEN_SHEETS[6] + "!B1:B" + (inputRow - 1), true);
			chart.getNSeries().setCategoryData(FPWTConstants.WT_HIDDEN_SHEETS[6] + "!A1:A" + (inputRow - 1));

			SeriesCollection serieses = chart.getNSeries();

			for(int i = 0; i < serieses.getCount(); i++) {
				Series series = serieses.get(i);
				ChartPointCollection chartPoints = series.getPoints();
				for(int j = 0; j < chartPoints.getCount(); j++) {
					ChartPoint chartPoint = chartPoints.get(j);
					chartPoint.getArea().setForegroundColor(Color.fromArgb(120, 168, 255));
				}

			}

			DataLabels dataLabels = chart.getNSeries().get(0).getDataLabels();
			dataLabels.setShowValue(true);
			dataLabels.setAutomaticSize(true);
			newWorksheet.getWorkbook().getWorksheets().get(FPWTConstants.WT_HIDDEN_SHEETS[6]).setVisible(false);

		}

		if(listFolioDtls.size() >= 10 && listFolioDtls.size() <= 20) {
			worksheet.getCells().insertRows(rowID + 2, 10);
		} else if(listFolioDtls.size() >= 21 && listFolioDtls.size() <= 30) {
			worksheet.getCells().insertRows(rowID + 2, 20);
		} else if(listFolioDtls.size() >= 31) {
			worksheet.getCells().insertRows(rowID + 2, 30);
		}
		LOGGER.info("PDF Generated Successfully.");
	}

	/**
	 * @param debtData
	 * @param worksheet
	 * @param rowID
	 * @param folioData
	 * @param minDebtDate
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private int generateDebtChart(LinkedHashMap<String, Object> debtData, Worksheet worksheet, int rowID, ArrayList<AssetFolioData> folioData,
			Date minDebtDate) throws Exception {
		for(Map.Entry<String, Object> map1:debtData.entrySet()) {
			if(map1.getKey().equalsIgnoreCase("AssetQuality")) {
				Chart chart = worksheet.getCharts().get(3);
				chart.getNSeries().clear();
				int inputRow = 1;
				LinkedHashMap<String, Double> map2 = (LinkedHashMap<String, Double>) map1.getValue();
				if(map2.size() > 0) {
					Worksheet newWorksheet = worksheet.getWorkbook().getWorksheets().add(FPWTConstants.WT_HIDDEN_SHEETS[5]);
					for(Map.Entry<String, Double> assetQuality:map2.entrySet()) {
						newWorksheet.getWorkbook().getWorksheets().get(FPWTConstants.WT_HIDDEN_SHEETS[5]).getCells().get("A" + inputRow)
								.putValue(assetQuality.getKey());
						newWorksheet.getWorkbook().getWorksheets().get(FPWTConstants.WT_HIDDEN_SHEETS[5]).getCells().get("B" + inputRow)
								.putValue(assetQuality.getValue() * 100);
						inputRow++;
					}
					chart.getNSeries().add(FPWTConstants.WT_HIDDEN_SHEETS[5] + "!B1:B" + (inputRow - 1), true);
					chart.getNSeries().setCategoryData(FPWTConstants.WT_HIDDEN_SHEETS[5] + "!A1:A" + (inputRow - 1));
					chart.setShowLegend(true);
					DataLabels dataLabels = chart.getNSeries().get(0).getDataLabels();
					dataLabels.setShowPercentage(true);
					dataLabels.setPosition(LabelPositionType.INSIDE_BASE);
					newWorksheet.getWorkbook().getWorksheets().get(FPWTConstants.WT_HIDDEN_SHEETS[5]).setVisible(false);

				}
			} else if(map1.getKey().equalsIgnoreCase("KeyIndicator")) {
				ArrayList<String> map2 = (ArrayList<String>) map1.getValue();
				if(map2.size() > 0) {
					int i = 1;
					for(String string:map2) {
						String[] newVal = string.split("_");
						if(i <= 2) {
							if(map2.size() > 1) {

								String schName = getSchemeName(newVal[0].substring(1), folioData);
								if(schName.toCharArray().length >= 60) {
									worksheet.getCells().setRowHeight(rowID - 1, 40);
								}

								worksheet.getCells().get("D" + rowID).putValue(schName);
								worksheet.getCells().get("E" + rowID).putValue(newVal[1]);
								worksheet.getCells().get("F" + rowID).putValue(newVal[2]);
							} else {

								String schName = getSchemeName(newVal[0].substring(1), folioData);
								if(schName.toCharArray().length >= 60) {
									worksheet.getCells().setRowHeight(rowID - 1, 40);
								}

								worksheet.getCells().get("D" + rowID).putValue(schName);
								worksheet.getCells().get("E" + rowID).putValue(newVal[1]);
								worksheet.getCells().get("F" + rowID).putValue(newVal[2]);

								worksheet.getCells().get("D" + (rowID + 1)).putValue("");
								worksheet.getCells().get("E" + (rowID + 1)).putValue("");
								worksheet.getCells().get("F" + (rowID + 1)).putValue("");
								rowID = rowID + 1;
							}
						} else {
							worksheet.getCells().insertRows(rowID - 1, 1);
							String schName = getSchemeName(newVal[0].substring(1), folioData);
							if(schName.toCharArray().length >= 60) {
								worksheet.getCells().setRowHeight(rowID - 1, 40);
							}

							worksheet.getCells().get("D" + rowID).putValue(schName);
							worksheet.getCells().get("E" + rowID).putValue(newVal[1]);
							worksheet.getCells().get("F" + rowID).putValue(newVal[2]);
						}
						rowID++;
						i++;

					}
				} else {
					worksheet.getCells().get("D" + rowID).putValue("");
					worksheet.getCells().get("E" + rowID).putValue("");
					worksheet.getCells().get("F" + rowID).putValue("");
					worksheet.getCells().get("D" + (rowID + 1)).putValue("");
					worksheet.getCells().get("E" + (rowID + 1)).putValue("");
					worksheet.getCells().get("F" + (rowID + 1)).putValue("");
					rowID = rowID + 2;
				}
			}
		}
		worksheet.getCells().get("E" + (rowID + 1)).putValue(Utility.formattedDate(minDebtDate));
		rowID = rowID + 4;

		for(Map.Entry<String, Object> map1:debtData.entrySet()) {
			if(map1.getKey().equalsIgnoreCase("SecurityClass")) {
				LinkedHashMap<String, Double> map2 = (LinkedHashMap<String, Double>) map1.getValue();
				if(map2.size() > 0) {
					int i = 1;
					for(Map.Entry<String, Double> assetQuality:map2.entrySet()) {
						if(i <= 2) {
							if(map2.size() > 1) {
								worksheet.getCells().get("D" + rowID).putValue(assetQuality.getKey());
								worksheet.getCells().get("E" + rowID).putValue(assetQuality.getValue() / 100);
							} else {
								worksheet.getCells().get("D" + rowID).putValue(assetQuality.getKey());
								worksheet.getCells().get("E" + rowID).putValue(assetQuality.getValue() / 100);
								worksheet.getCells().get("D" + (rowID + 1)).putValue("");
								worksheet.getCells().get("E" + (rowID + 1)).putValue("");
								rowID = rowID + 1;
							}
						} else {
							worksheet.getCells().insertRows(rowID - 1, 1);
							worksheet.getCells().get("D" + rowID).putValue(assetQuality.getKey());
							worksheet.getCells().get("E" + rowID).putValue(assetQuality.getValue() / 100);
						}
						rowID++;
						i++;
					}
				} else {
					worksheet.getCells().get("D" + rowID).putValue("");
					worksheet.getCells().get("E" + rowID).putValue("");
					worksheet.getCells().get("D" + (rowID + 1)).putValue("");
					worksheet.getCells().get("E" + (rowID + 1)).putValue("");
				}
			}
		}
		return rowID + 9;
	}

	private String getSchemeName(String schCode, ArrayList<AssetFolioData> folioData) {
		for(AssetFolioData assetFolioData:folioData) {
			if(assetFolioData.getSchcode().equalsIgnoreCase(schCode))
				return assetFolioData.getSch_name();
		}

		return null;
	}

	/**
	 * Generate Equity Chart (Top Ten and Top Five)
	 * 
	 * @param equityData
	 * @param worksheet
	 * @param minEquityDate
	 * @param rowID
	 */
	private int generatEquityChart(LinkedHashMap<String, LinkedHashMap<String, Double>> equityData, Worksheet worksheet, Date minEquityDate,
			int rowID) {
		LOGGER.info("Generate Equity Chart (Top Ten and Top Five)");
		worksheet.getCells().get("E" + rowID).putValue(minEquityDate);
		for(Map.Entry<String, LinkedHashMap<String, Double>> map1:equityData.entrySet()) {
			if(map1.getKey().equalsIgnoreCase("TopTenCompany")) {
				Chart chart = worksheet.getCharts().get(1);
				chart.getNSeries().clear();
				int inputRow = 1;
				LinkedHashMap<String, Double> map2 = map1.getValue();
				if(map2.size() > 0) {
					Worksheet newWorksheet = worksheet.getWorkbook().getWorksheets().add(FPWTConstants.WT_HIDDEN_SHEETS[3]);
					for(Map.Entry<String, Double> topTenData:map2.entrySet()) {
						newWorksheet.getWorkbook().getWorksheets().get(FPWTConstants.WT_HIDDEN_SHEETS[3]).getCells().get("A" + inputRow)
								.putValue(topTenData.getKey());
						newWorksheet.getWorkbook().getWorksheets().get(FPWTConstants.WT_HIDDEN_SHEETS[3]).getCells().get("B" + inputRow)
								.putValue(roundValue(topTenData.getValue()));
						inputRow++;
					}
					chart.getNSeries().add(FPWTConstants.WT_HIDDEN_SHEETS[3] + "!B1:B" + (inputRow - 1), true);
					chart.getNSeries().setCategoryData(FPWTConstants.WT_HIDDEN_SHEETS[3] + "!A1:A" + (inputRow - 1));

					SeriesCollection serieses = chart.getNSeries();

					for(int i = 0; i < serieses.getCount(); i++) {
						Series series = serieses.get(i);
						ChartPointCollection chartPoints = series.getPoints();
						for(int j = 0; j < chartPoints.getCount(); j++) {
							ChartPoint chartPoint = chartPoints.get(j);
							chartPoint.getArea().setForegroundColor(Color.fromArgb(255, 64, 0));
						}

					}

					DataLabels dataLabels = chart.getNSeries().get(0).getDataLabels();
					dataLabels.setShowValue(true);

					newWorksheet.getWorkbook().getWorksheets().get(FPWTConstants.WT_HIDDEN_SHEETS[3]).setVisible(false);
				}
			} else if(map1.getKey().equalsIgnoreCase("TopFiveIndustry")) {
				Chart chart = worksheet.getCharts().get(2);
				chart.getNSeries().clear();
				int inputRow = 1;
				LinkedHashMap<String, Double> map2 = map1.getValue();
				if(map2.size() > 0) {
					Worksheet newWorksheet = worksheet.getWorkbook().getWorksheets().add(FPWTConstants.WT_HIDDEN_SHEETS[4]);
					for(Map.Entry<String, Double> topFiveData:map2.entrySet()) {
						newWorksheet.getWorkbook().getWorksheets().get(FPWTConstants.WT_HIDDEN_SHEETS[4]).getCells().get("A" + inputRow)
								.putValue(topFiveData.getKey());
						newWorksheet.getWorkbook().getWorksheets().get(FPWTConstants.WT_HIDDEN_SHEETS[4]).getCells().get("B" + inputRow)
								.putValue(roundValue(topFiveData.getValue()));
						inputRow++;
					}
					chart.getNSeries().add(FPWTConstants.WT_HIDDEN_SHEETS[4] + "!B1:B" + (inputRow - 1), true);
					chart.getNSeries().setCategoryData(FPWTConstants.WT_HIDDEN_SHEETS[4] + "!A1:A" + (inputRow - 1));

					SeriesCollection serieses = chart.getNSeries();

					for(int i = 0; i < serieses.getCount(); i++) {
						Series series = serieses.get(i);
						ChartPointCollection chartPoints = series.getPoints();
						for(int j = 0; j < chartPoints.getCount(); j++) {
							ChartPoint chartPoint = chartPoints.get(j);
							chartPoint.getArea().setForegroundColor(Color.fromArgb(0, 46, 130));
						}

					}

					DataLabels dataLabels = chart.getNSeries().get(0).getDataLabels();
					dataLabels.setShowValue(true);

					newWorksheet.getWorkbook().getWorksheets().get(FPWTConstants.WT_HIDDEN_SHEETS[4]).setVisible(false);
				}
			}
		}
		return rowID + 20;
	}

	/**
	 * Calculate Fund Pulse
	 * 
	 * @param worksheet
	 * @param wtRequest
	 * @return
	 */
	private int fundPulseCalculation(Worksheet worksheet, WTRequest wtRequest) {
		LOGGER.info("Fund Pulse Calculation");
		ArrayList<AssetFolioData> totalEquityFund = new ArrayList<AssetFolioData>();
		ArrayList<AssetFolioData> totalDebtFund = new ArrayList<AssetFolioData>();
		ArrayList<AssetFolioData> totalLiquidFund = new ArrayList<AssetFolioData>();
		ArrayList<AssetFolioData> totalAdvisorSeries = new ArrayList<AssetFolioData>();
		ArrayList<AssetFolioData> totalBalHybr = new ArrayList<AssetFolioData>();

		Object[] equity = new Object[5];
		Object[] debt = new Object[5];
		Object[] liquid = new Object[5];
		Object[] advisor = new Object[5];
		Object[] balHybr = new Object[5];

		int rowID = 0;
		int flag = 0;
		ArrayList<AssetFolioData> listAssetFolio = wtRequest.getDetails().getListFolioDtls();
		listAssetFolio.forEach(assetFolio -> {
			if(assetFolio.getAsset_class().contains(FPWTConstants.WT_CONSTANTS[0])) {
				totalEquityFund.add(assetFolio);
			} else if(assetFolio.getAsset_class().contains(FPWTConstants.WT_CONSTANTS[1])) {
				totalDebtFund.add(assetFolio);
			} else if(assetFolio.getAsset_class().contains(FPWTConstants.WT_CONSTANTS[2])
					|| assetFolio.getAsset_class().contains(FPWTConstants.WT_CONSTANTS[3])) {
				totalLiquidFund.add(assetFolio);
			} else if(assetFolio.getAsset_class().contains(FPWTConstants.WT_CONSTANTS[4])) {
				totalAdvisorSeries.add(assetFolio);
			} else if(assetFolio.getAsset_class().contains(FPWTConstants.WT_CONSTANTS[5])
					|| assetFolio.getAsset_class().contains(FPWTConstants.WT_CONSTANTS[6])
					|| assetFolio.getAsset_class().contains(FPWTConstants.WT_CONSTANTS[7])) {
				totalBalHybr.add(assetFolio);
			}
		});

		// For Equity
		if(totalEquityFund.size() > 0) {
			equity = calculateEquityFund(worksheet, totalEquityFund, wtRequest.getMarket_value());
			rowID = (int) equity[0];
			flag = 0;
		} else {
			worksheet.getCells().deleteRows(7, 3);
			rowID = 9;
			flag = 1;
		}

		// For Debt
		if(totalDebtFund.size() > 0) {
			debt = calculateDebtFund(worksheet, totalDebtFund, rowID, wtRequest.getMarket_value());
			rowID = (int) debt[0];
			flag = 0;
		} else {
			if(flag == 1) {
				worksheet.getCells().deleteRows(7, 3);
				rowID = 9;
				flag = 1;
			} else {
				worksheet.getCells().deleteRows(rowID - 3, 3);
				flag = 0;
			}

		}

		// For Liquid
		if(totalLiquidFund.size() > 0) {
			liquid = calculateLiquidFund(worksheet, totalLiquidFund, rowID, wtRequest.getMarket_value());
			rowID = (int) liquid[0];
			flag = 0;
		} else {
			if(flag == 1) {
				worksheet.getCells().deleteRows(7, 3);
				rowID = 9;
				flag = 1;
			} else {
				worksheet.getCells().deleteRows(rowID - 3, 3);
				flag = 0;
			}

		}

		// For Advisor
		if(totalAdvisorSeries.size() > 0) {
			advisor = calculateAdvisorFund(worksheet, totalAdvisorSeries, rowID, wtRequest.getMarket_value());
			rowID = (int) advisor[0];
			flag = 0;
		} else {
			if(flag == 1) {
				worksheet.getCells().deleteRows(7, 3);
				rowID = 9;
				flag = 1;
			} else {
				worksheet.getCells().deleteRows(rowID - 3, 3);
				flag = 0;
			}

		}

		// For BalHybrAllocation
		if(totalBalHybr.size() > 0) {
			balHybr = calculateBalHybrAllocation(worksheet, totalBalHybr, rowID, wtRequest.getMarket_value());
			rowID = (int) balHybr[0];
			flag = 0;
		} else {
			if(flag == 1) {
				worksheet.getCells().deleteRows(7, 3);
				rowID = 9;
				flag = 1;
			} else {
				worksheet.getCells().deleteRows(rowID - 3, 3);
				flag = 0;
			}

		}

		// Total Summary
		rowID = calculateTotalSummary(equity, debt, liquid, advisor, balHybr, rowID, worksheet);

		return rowID + 7;
	}

	/**
	 * @param equity
	 * @param debt
	 * @param liquid
	 * @param advisor
	 * @param balHybr
	 * @param rowID
	 * @param worksheet
	 * @return
	 */
	private int calculateTotalSummary(Object[] equity, Object[] debt, Object[] liquid, Object[] advisor, Object[] balHybr, int rowID,
			Worksheet worksheet) {

		double grandCostTotal = 0.00;
		double grandCurrentTotal = 0.00;
		double grandProfitLossTotal = 0.00;
		double grandExposure = 0.00;

		if(equity[1] != null) {
			worksheet.getCells().get("I" + rowID).putValue(equity[1]);
			worksheet.getCells().get("J" + rowID).putValue(equity[2]);
			worksheet.getCells().get("K" + rowID).putValue(equity[3]);
			worksheet.getCells().get("M" + rowID).putValue(equity[4]);
			grandCostTotal = grandCostTotal + (double) equity[1];
			grandCurrentTotal = grandCurrentTotal + (double) equity[2];
			grandProfitLossTotal = grandProfitLossTotal + (double) equity[3];
			grandExposure = grandExposure + (double) equity[4];
		} else {
			worksheet.getCells().get("I" + rowID).putValue(0.00);
			worksheet.getCells().get("J" + rowID).putValue(0.00);
			worksheet.getCells().get("K" + rowID).putValue(0.00);
			worksheet.getCells().get("M" + rowID).putValue(0.00);
		}
		rowID++;

		if(debt[1] != null) {
			worksheet.getCells().get("I" + rowID).putValue(debt[1]);
			worksheet.getCells().get("J" + rowID).putValue(debt[2]);
			worksheet.getCells().get("K" + rowID).putValue(debt[3]);
			worksheet.getCells().get("M" + rowID).putValue(debt[4]);
			grandCostTotal = grandCostTotal + (double) debt[1];
			grandCurrentTotal = grandCurrentTotal + (double) debt[2];
			grandProfitLossTotal = grandProfitLossTotal + (double) debt[3];
			grandExposure = grandExposure + (double) debt[4];
		} else {
			worksheet.getCells().get("I" + rowID).putValue(0.00);
			worksheet.getCells().get("J" + rowID).putValue(0.00);
			worksheet.getCells().get("K" + rowID).putValue(0.00);
			worksheet.getCells().get("M" + rowID).putValue(0.00);
		}
		rowID++;

		if(liquid[1] != null) {
			worksheet.getCells().get("I" + rowID).putValue(liquid[1]);
			worksheet.getCells().get("J" + rowID).putValue(liquid[2]);
			worksheet.getCells().get("K" + rowID).putValue(liquid[3]);
			worksheet.getCells().get("M" + rowID).putValue(liquid[4]);
			grandCostTotal = grandCostTotal + (double) liquid[1];
			grandCurrentTotal = grandCurrentTotal + (double) liquid[2];
			grandProfitLossTotal = grandProfitLossTotal + (double) liquid[3];
			grandExposure = grandExposure + (double) liquid[4];
		} else {
			worksheet.getCells().get("I" + rowID).putValue(0.00);
			worksheet.getCells().get("J" + rowID).putValue(0.00);
			worksheet.getCells().get("K" + rowID).putValue(0.00);
			worksheet.getCells().get("M" + rowID).putValue(0.00);
		}

		rowID++;

		if(advisor[1] != null) {
			worksheet.getCells().get("I" + rowID).putValue(advisor[1]);
			worksheet.getCells().get("J" + rowID).putValue(advisor[2]);
			worksheet.getCells().get("K" + rowID).putValue(advisor[3]);
			worksheet.getCells().get("M" + rowID).putValue(advisor[4]);
			grandCostTotal = grandCostTotal + (double) advisor[1];
			grandCurrentTotal = grandCurrentTotal + (double) advisor[2];
			grandProfitLossTotal = grandProfitLossTotal + (double) advisor[3];
			grandExposure = grandExposure + (double) advisor[4];
		} else {
			worksheet.getCells().get("I" + rowID).putValue(0.00);
			worksheet.getCells().get("J" + rowID).putValue(0.00);
			worksheet.getCells().get("K" + rowID).putValue(0.00);
			worksheet.getCells().get("M" + rowID).putValue(0.00);
		}
		rowID++;

		if(balHybr[1] != null) {
			worksheet.getCells().get("I" + rowID).putValue(balHybr[1]);
			worksheet.getCells().get("J" + rowID).putValue(balHybr[2]);
			worksheet.getCells().get("K" + rowID).putValue(balHybr[3]);
			worksheet.getCells().get("M" + rowID).putValue(balHybr[4]);
			grandCostTotal = grandCostTotal + (double) balHybr[1];
			grandCurrentTotal = grandCurrentTotal + (double) balHybr[2];
			grandProfitLossTotal = grandProfitLossTotal + (double) balHybr[3];
			grandExposure = grandExposure + (double) balHybr[4];
		} else {
			worksheet.getCells().get("I" + rowID).putValue(0.00);
			worksheet.getCells().get("J" + rowID).putValue(0.00);
			worksheet.getCells().get("K" + rowID).putValue(0.00);
			worksheet.getCells().get("M" + rowID).putValue(0.00);
		}
		rowID++;

		worksheet.getCells().get("I" + rowID).putValue(grandCostTotal);
		worksheet.getCells().get("J" + rowID).putValue(grandCurrentTotal);
		worksheet.getCells().get("K" + rowID).putValue(grandProfitLossTotal);
		worksheet.getCells().get("M" + rowID).putValue(grandExposure);

		return rowID;
	}

	/**
	 * @param worksheet
	 * @param totalBalHybr
	 * @param size
	 * @param mrktValue
	 * @return
	 */
	private Object[] calculateBalHybrAllocation(Worksheet worksheet, ArrayList<AssetFolioData> totalBalHybr, int size, String mrktValue) {
		int i = 0;
		int count = 0;
		count = size;
		int rowID = count;

		double costOfCurrentMarket = 0.00;
		double currentMarket = 0.00;
		double profitLoss = 0.00;
		double exposure = 0.00;

		Object[] obj = new Object[5];

		for(AssetFolioData assetFolioData:totalBalHybr) {
			if(i == 0) {
				worksheet.getCells().get("D" + (i + count)).putValue(assetFolioData.getSch_name());
				worksheet.getCells().get("I" + (i + count)).putValue(assetFolioData.getCost_value());
				worksheet.getCells().get("J" + (i + count)).putValue(assetFolioData.getCurrent_value());
				worksheet.getCells().get("K" + (i + count)).putValue(assetFolioData.getGain_loss());
				worksheet.getCells().get("L" + (i + count)).putValue(percentageCal(assetFolioData.getXirr()));
				worksheet.getCells().get("M" + (i + count)).putValue((assetFolioData.getCurrent_value() / Double.parseDouble(mrktValue)) * 100);
			} else {
				worksheet.getCells().insertRow(rowID);
				worksheet.getCells().get("D" + (i + count)).putValue(assetFolioData.getSch_name());
				worksheet.getCells().get("I" + (i + count)).putValue(assetFolioData.getCost_value());
				worksheet.getCells().get("J" + (i + count)).putValue(assetFolioData.getCurrent_value());
				worksheet.getCells().get("K" + (i + count)).putValue(assetFolioData.getGain_loss());
				worksheet.getCells().get("L" + (i + count)).putValue(percentageCal(assetFolioData.getXirr()));
				worksheet.getCells().get("M" + (i + count)).putValue((assetFolioData.getCurrent_value() / Double.parseDouble(mrktValue)) * 100);
				rowID++;
			}

			costOfCurrentMarket = costOfCurrentMarket + assetFolioData.getCost_value();
			currentMarket = currentMarket + assetFolioData.getCurrent_value();
			profitLoss = profitLoss + assetFolioData.getGain_loss();
			exposure = exposure + (assetFolioData.getCurrent_value() / Double.parseDouble(mrktValue)) * 100;

			i++;
		}

		worksheet.getCells().get("I" + (i + count)).putValue(costOfCurrentMarket);
		worksheet.getCells().get("J" + (i + count)).putValue(currentMarket);
		worksheet.getCells().get("K" + (i + count)).putValue(profitLoss);
		worksheet.getCells().get("M" + (i + count)).putValue(exposure);

		obj[0] = rowID + 3;
		obj[1] = costOfCurrentMarket;
		obj[2] = currentMarket;
		obj[3] = profitLoss;
		obj[4] = exposure;

		return obj;
	}

	/**
	 * @param worksheet
	 * @param totalLiquidFund
	 * @param size
	 * @param mrktValue
	 * @return
	 */
	private Object[] calculateLiquidFund(Worksheet worksheet, ArrayList<AssetFolioData> totalLiquidFund, int size, String mrktValue) {
		int i = 0;
		int count = 0;
		count = size;
		int rowID = count;

		double costOfCurrentMarket = 0.00;
		double currentMarket = 0.00;
		double profitLoss = 0.00;
		double exposure = 0.00;

		Object[] obj = new Object[5];

		for(AssetFolioData assetFolioData:totalLiquidFund) {
			if(i == 0) {
				worksheet.getCells().get("D" + (i + count)).putValue(assetFolioData.getSch_name());
				worksheet.getCells().get("I" + (i + count)).putValue(assetFolioData.getCost_value());
				worksheet.getCells().get("J" + (i + count)).putValue(assetFolioData.getCurrent_value());
				worksheet.getCells().get("K" + (i + count)).putValue(assetFolioData.getGain_loss());
				worksheet.getCells().get("L" + (i + count)).putValue(percentageCal(assetFolioData.getXirr()));
				worksheet.getCells().get("M" + (i + count)).putValue((assetFolioData.getCurrent_value() / Double.parseDouble(mrktValue)) * 100);
			} else {
				worksheet.getCells().insertRow(rowID);
				worksheet.getCells().get("D" + (i + count)).putValue(assetFolioData.getSch_name());
				worksheet.getCells().get("I" + (i + count)).putValue(assetFolioData.getCost_value());
				worksheet.getCells().get("J" + (i + count)).putValue(assetFolioData.getCurrent_value());
				worksheet.getCells().get("K" + (i + count)).putValue(assetFolioData.getGain_loss());
				worksheet.getCells().get("L" + (i + count)).putValue(percentageCal(assetFolioData.getXirr()));
				worksheet.getCells().get("M" + (i + count)).putValue((assetFolioData.getCurrent_value() / Double.parseDouble(mrktValue)) * 100);
				rowID++;
			}

			costOfCurrentMarket = costOfCurrentMarket + assetFolioData.getCost_value();
			currentMarket = currentMarket + assetFolioData.getCurrent_value();
			profitLoss = profitLoss + assetFolioData.getGain_loss();
			exposure = exposure + (assetFolioData.getCurrent_value() / Double.parseDouble(mrktValue)) * 100;

			i++;
		}

		worksheet.getCells().get("I" + (i + count)).putValue(costOfCurrentMarket);
		worksheet.getCells().get("J" + (i + count)).putValue(currentMarket);
		worksheet.getCells().get("K" + (i + count)).putValue(profitLoss);
		worksheet.getCells().get("M" + (i + count)).putValue(exposure);

		obj[0] = rowID + 3;
		obj[1] = costOfCurrentMarket;
		obj[2] = currentMarket;
		obj[3] = profitLoss;
		obj[4] = exposure;

		return obj;
	}

	/**
	 * @param worksheet
	 * @param totalAdvisorFund
	 * @param size
	 * @param mrktValue
	 * @return
	 */
	private Object[] calculateAdvisorFund(Worksheet worksheet, ArrayList<AssetFolioData> totalAdvisorFund, int size, String mrktValue) {
		int i = 0;
		int count = 0;
		count = size;
		int rowID = count;

		double costOfCurrentMarket = 0.00;
		double currentMarket = 0.00;
		double profitLoss = 0.00;
		double exposure = 0.00;

		Object[] obj = new Object[5];

		for(AssetFolioData assetFolioData:totalAdvisorFund) {
			if(i == 0) {
				worksheet.getCells().get("D" + (i + count)).putValue(assetFolioData.getSch_name());
				worksheet.getCells().get("I" + (i + count)).putValue(assetFolioData.getCost_value());
				worksheet.getCells().get("J" + (i + count)).putValue(assetFolioData.getCurrent_value());
				worksheet.getCells().get("K" + (i + count)).putValue(assetFolioData.getGain_loss());
				worksheet.getCells().get("L" + (i + count)).putValue(percentageCal(assetFolioData.getXirr()));
				worksheet.getCells().get("M" + (i + count)).putValue((assetFolioData.getCurrent_value() / Double.parseDouble(mrktValue)) * 100);
			} else {
				worksheet.getCells().insertRow(rowID);
				worksheet.getCells().get("D" + (i + count)).putValue(assetFolioData.getSch_name());
				worksheet.getCells().get("I" + (i + count)).putValue(assetFolioData.getCost_value());
				worksheet.getCells().get("J" + (i + count)).putValue(assetFolioData.getCurrent_value());
				worksheet.getCells().get("K" + (i + count)).putValue(assetFolioData.getGain_loss());
				worksheet.getCells().get("L" + (i + count)).putValue(percentageCal(assetFolioData.getXirr()));
				worksheet.getCells().get("M" + (i + count)).putValue((assetFolioData.getCurrent_value() / Double.parseDouble(mrktValue)) * 100);
				rowID++;
			}

			costOfCurrentMarket = costOfCurrentMarket + assetFolioData.getCost_value();
			currentMarket = currentMarket + assetFolioData.getCurrent_value();
			profitLoss = profitLoss + assetFolioData.getGain_loss();
			exposure = exposure + (assetFolioData.getCurrent_value() / Double.parseDouble(mrktValue)) * 100;

			i++;
		}

		worksheet.getCells().get("I" + (i + count)).putValue(costOfCurrentMarket);
		worksheet.getCells().get("J" + (i + count)).putValue(currentMarket);
		worksheet.getCells().get("K" + (i + count)).putValue(profitLoss);
		worksheet.getCells().get("M" + (i + count)).putValue(exposure);

		obj[0] = rowID + 3;
		obj[1] = costOfCurrentMarket;
		obj[2] = currentMarket;
		obj[3] = profitLoss;
		obj[4] = exposure;

		return obj;

	}

	/**
	 * @param worksheet
	 * @param totalDebtFund
	 * @param totalEquiFund
	 * @param mrktValue
	 * @return
	 */
	private Object[] calculateDebtFund(Worksheet worksheet, ArrayList<AssetFolioData> totalDebtFund, int totalEquiFund, String mrktValue) {
		int i = 0;
		int count = 0;
		count = totalEquiFund;
		int rowID = count;

		double costOfCurrentMarket = 0.00;
		double currentMarket = 0.00;
		double profitLoss = 0.00;
		double exposure = 0.00;

		Object[] obj = new Object[5];

		for(AssetFolioData assetFolioData:totalDebtFund) {
			if(i == 0) {
				worksheet.getCells().get("D" + (i + count)).putValue(assetFolioData.getSch_name());
				worksheet.getCells().get("I" + (i + count)).putValue(assetFolioData.getCost_value());
				worksheet.getCells().get("J" + (i + count)).putValue(assetFolioData.getCurrent_value());
				worksheet.getCells().get("K" + (i + count)).putValue(assetFolioData.getGain_loss());
				worksheet.getCells().get("L" + (i + count)).putValue(percentageCal(assetFolioData.getXirr()));
				worksheet.getCells().get("M" + (i + count)).putValue((assetFolioData.getCurrent_value() / Double.parseDouble(mrktValue)) * 100);
			} else {
				worksheet.getCells().insertRow(rowID);
				worksheet.getCells().get("D" + (i + count)).putValue(assetFolioData.getSch_name());
				worksheet.getCells().get("I" + (i + count)).putValue(assetFolioData.getCost_value());
				worksheet.getCells().get("J" + (i + count)).putValue(assetFolioData.getCurrent_value());
				worksheet.getCells().get("K" + (i + count)).putValue(assetFolioData.getGain_loss());
				worksheet.getCells().get("L" + (i + count)).putValue(percentageCal(assetFolioData.getXirr()));
				worksheet.getCells().get("M" + (i + count)).putValue((assetFolioData.getCurrent_value() / Double.parseDouble(mrktValue)) * 100);
				rowID++;
			}

			costOfCurrentMarket = costOfCurrentMarket + assetFolioData.getCost_value();
			currentMarket = currentMarket + assetFolioData.getCurrent_value();
			profitLoss = profitLoss + assetFolioData.getGain_loss();
			exposure = exposure + (assetFolioData.getCurrent_value() / Double.parseDouble(mrktValue)) * 100;

			i++;
		}

		worksheet.getCells().get("I" + (i + count)).putValue(costOfCurrentMarket);
		worksheet.getCells().get("J" + (i + count)).putValue(currentMarket);
		worksheet.getCells().get("K" + (i + count)).putValue(profitLoss);
		worksheet.getCells().get("M" + (i + count)).putValue(exposure);

		obj[0] = rowID + 3;
		obj[1] = costOfCurrentMarket;
		obj[2] = currentMarket;
		obj[3] = profitLoss;
		obj[4] = exposure;

		return obj;

	}

	/**
	 * @param worksheet
	 * @param totalEquityFund
	 * @param mrktValue
	 * @return
	 */
	private Object[] calculateEquityFund(Worksheet worksheet, ArrayList<AssetFolioData> totalEquityFund, String mrktValue) {
		int i = 0;
		int rowID = 9;
		double costOfCurrentMarket = 0.00;
		double currentMarket = 0.00;
		double profitLoss = 0.00;
		double exposure = 0.00;
		Object[] obj = new Object[5];
		for(AssetFolioData assetFolioData:totalEquityFund) {
			if(i == 0) {
				worksheet.getCells().get("D" + (i + 9)).putValue(assetFolioData.getSch_name());
				worksheet.getCells().get("I" + (i + 9)).putValue(assetFolioData.getCost_value());
				worksheet.getCells().get("J" + (i + 9)).putValue(assetFolioData.getCurrent_value());
				worksheet.getCells().get("K" + (i + 9)).putValue(assetFolioData.getGain_loss());
				worksheet.getCells().get("L" + (i + 9)).putValue(percentageCal(assetFolioData.getXirr()));
				worksheet.getCells().get("M" + (i + 9)).putValue((assetFolioData.getCurrent_value() / Double.parseDouble(mrktValue)) * 100);
			} else {
				worksheet.getCells().insertRow(rowID);
				worksheet.getCells().get("D" + (i + 9)).putValue(assetFolioData.getSch_name());
				worksheet.getCells().get("I" + (i + 9)).putValue(assetFolioData.getCost_value());
				worksheet.getCells().get("J" + (i + 9)).putValue(assetFolioData.getCurrent_value());
				worksheet.getCells().get("K" + (i + 9)).putValue(assetFolioData.getGain_loss());
				worksheet.getCells().get("L" + (i + 9)).putValue(percentageCal(assetFolioData.getXirr()));
				worksheet.getCells().get("M" + (i + 9)).putValue((assetFolioData.getCurrent_value() / Double.parseDouble(mrktValue)) * 100);
				rowID++;
			}

			costOfCurrentMarket = costOfCurrentMarket + assetFolioData.getCost_value();
			currentMarket = currentMarket + assetFolioData.getCurrent_value();
			profitLoss = profitLoss + assetFolioData.getGain_loss();
			exposure = exposure + (assetFolioData.getCurrent_value() / Double.parseDouble(mrktValue)) * 100;

			i++;
		}

		worksheet.getCells().get("I" + (i + 9)).putValue(costOfCurrentMarket);
		worksheet.getCells().get("J" + (i + 9)).putValue(currentMarket);
		worksheet.getCells().get("K" + (i + 9)).putValue(profitLoss);
		worksheet.getCells().get("M" + (i + 9)).putValue(exposure);

		obj[0] = rowID + 3;
		obj[1] = costOfCurrentMarket;
		obj[2] = currentMarket;
		obj[3] = profitLoss;
		obj[4] = exposure;

		return obj;
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
	 * @param folder
	 * @return
	 */
	private List<String> listFilesForFolder(File folder) {

		List<String> fileNamesList = new ArrayList<String>();
		for(File fileEntry:folder.listFiles()) {
			if(fileEntry.isDirectory()) {
				LOGGER.info("Directory is found- verify whether it is output file or not.");
			} else {
				LOGGER.info(fileEntry.getName());
				fileNamesList.add(fileEntry.getName());
			}
		}
		return fileNamesList;
	}

	/**
	 * Remove percentage
	 * 
	 * @param val
	 * @return
	 */
	public Double percentageCal(String val) {
		BigDecimal d = new BigDecimal(val.trim().replace("%", ""));

		return d.doubleValue();
	}

	/**
	 * Equity & Debt Sch Code
	 * 
	 * @param listFolioDtls
	 * @return
	 */
	private String[] calculateScheduleCode(ArrayList<AssetFolioData> listFolioDtls) {
		String equityData = "";
		String debtData = "";
		String allData = "";
		String[] finalVal = new String[3];
		LOGGER.info("Equity & Debt Scheme Code Calculation");
		for(AssetFolioData assetFolioData:listFolioDtls) {
			if(assetFolioData.getAsset_class().contains(FPWTConstants.WT_CONSTANTS[0])) {
				if(equityData.equals("")) {
					equityData = "'P" + assetFolioData.getSchcode() + "'";
				} else {
					equityData = equityData + ",'P" + assetFolioData.getSchcode() + "'";
				}
			} else if(assetFolioData.getAsset_class().contains(FPWTConstants.WT_CONSTANTS[1])) {
				if(debtData.equals("")) {
					debtData = "'P" + assetFolioData.getSchcode() + "'";
				} else {
					debtData = debtData + ",'P" + assetFolioData.getSchcode() + "'";
				}
			}

			// Calculation for all code
			if(allData.equals("")) {
				allData = "'P" + assetFolioData.getSchcode() + "'";
			} else {
				allData = allData + ",'P" + assetFolioData.getSchcode() + "'";
			}

		}

		finalVal[0] = equityData;
		finalVal[1] = debtData;
		finalVal[2] = allData;

		return finalVal;
	}

	/**
	 * @param val
	 * @return round value
	 */
	public double roundValue(double val) {
		double d = Math.round(val * 100.0) / 100.0;
		return d;
	}

}
