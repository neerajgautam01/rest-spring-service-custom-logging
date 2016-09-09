/**
 * 
 */
package com.fpwt.wtdao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;

import com.fpwt.service.beans.PerformanceBean;

/**
 * @author c-NikunjP
 *
 */
public interface WTDao {

	/**
	 * Calculate Equity Data
	 * 
	 * @param data
	 * @param minEquityDate 
	 * @return
	 */
	LinkedHashMap<String, LinkedHashMap<String, Double>> calculateEquityData(String data, Date minEquityDate);

	/**
	 * Calculate Debt Data
	 * 
	 * @param string
	 * @param minDebtDate 
	 * @return
	 */
	LinkedHashMap<String, Object> calculateDebtData(String string, Date minDebtDate);

	/**
	 * Calculate Performance
	 * 
	 * @param inputData
	 * @return
	 * @throws SQLException
	 */
	ArrayList<PerformanceBean> getPerformanceReport(String inputData) throws SQLException;

	/**
	 * @param string
	 * @return
	 */
	Date getMinDateForEquity(String string);

	/**
	 * @param string
	 * @return
	 */
	Date getMinDateForDebt(String string);

	/**
	 * @param string
	 * @param formattedDate
	 * @param length
	 * @return
	 */
	LinkedHashMap<String, Double> getSecurityClass(String schmCode, String formattedDate, int length);

	/**
	 * @param formattedDate
	 * @param string
	 * @return
	 */
	String getAvailableDebtSchemes(String formattedDate, String string);

	/**
	 * @param schemes
	 * @param formattedDate
	 * @param length
	 * @return
	 */
	LinkedHashMap<String, Double> getSecurityClassForPie(String schemes, String formattedDate, int length);

}
