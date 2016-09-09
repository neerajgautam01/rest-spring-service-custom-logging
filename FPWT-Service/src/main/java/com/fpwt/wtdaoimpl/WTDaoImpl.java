/**
 * 
 */
package com.fpwt.wtdaoimpl;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import com.fpwt.service.beans.PerformanceBean;
import com.fpwt.service.common.Utility;
import com.fpwt.wtdao.WTDao;

import oracle.jdbc.OracleTypes;

/**
 * @author c-NikunjP
 *
 */
@Repository
public class WTDaoImpl implements WTDao {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	private static Logger LOGGER = LoggerFactory.getLogger(WTDaoImpl.class.getName());

	/**
	 * Calculate Performance
	 * 
	 * @param data
	 * @return
	 * @throws SQLException
	 */
	@Override
	public ArrayList<PerformanceBean> getPerformanceReport(String inputData) throws SQLException {
		CallableStatement callableStatement = null;
		ResultSet rs = null;
		ArrayList<PerformanceBean> performaceData = new ArrayList<PerformanceBean>();
		String getDBUSERCursorSql = "{call CRISILFA.SP_GetP2POfSelectedSchemes(?,?)}";
		callableStatement = jdbcTemplate.getDataSource().getConnection().prepareCall(getDBUSERCursorSql);
		callableStatement.setString(1, inputData);
		callableStatement.registerOutParameter(2, OracleTypes.CURSOR);
		callableStatement.executeUpdate();
		rs = (ResultSet) callableStatement.getObject(2);
		while(rs.next()) {
			PerformanceBean bean = new PerformanceBean();
			bean.setIndexName(rs.getString(1));
			bean.setsType(rs.getString(2));
			bean.setRtaCode(rs.getString(3).substring(1));
			bean.setThreeMonthReturn(rs.getDouble(4));
			bean.setSixMonthReturn(rs.getDouble(5));
			bean.setOneYearReturn(rs.getDouble(6));
			bean.setThreeYearReturn(rs.getDouble(7));
			bean.setFiveYearReturn(rs.getDouble(8));
			performaceData.add(bean);
		}

		return performaceData;
	}

	/**
	 * Calculate Equity Data
	 * 
	 * @param data
	 * @return
	 */
	@Override
	public LinkedHashMap<String, LinkedHashMap<String, Double>> calculateEquityData(String data, Date fDate) {
		LOGGER.info("Calculate Equity Data");
		LinkedHashMap<String, LinkedHashMap<String, Double>> equityCal = new LinkedHashMap<String, LinkedHashMap<String, Double>>();
		LinkedHashMap<String, Double> companyExposureTopTen = new LinkedHashMap<String, Double>();
		LinkedHashMap<String, Double> companyExposureTopFive = new LinkedHashMap<String, Double>();

		// Format Equity Date as per req.
		String formattedDate = Utility.formattedDate(fDate);

		// Get availabe scheme code for equity
		String schemes = getAvailableEquitySchemes(formattedDate, data);

		// Calculate Equity for Top Ten and Top Five
		String[] dataLength = data.split(",");
		if(schemes != null) {
			companyExposureTopTen = getDetailsOfTopTen(schemes, formattedDate, dataLength.length);
			companyExposureTopFive = getDetailsOfTopFive(schemes, formattedDate, dataLength.length);
		}

		equityCal.put("TopTenCompany", companyExposureTopTen);
		equityCal.put("TopFiveIndustry", companyExposureTopFive);

		return equityCal;
	}

	/**
	 * Calculate Debt Data
	 * 
	 * @param string
	 * @return
	 */
	@Override
	public LinkedHashMap<String, Object> calculateDebtData(String data, Date dDate) {
		LOGGER.info("Calculate Debt Data");
		LinkedHashMap<String, Object> debtCal = new LinkedHashMap<String, Object>();
		LinkedHashMap<String, Double> assetQuality = new LinkedHashMap<String, Double>();
		LinkedHashMap<String, Double> securityClass = new LinkedHashMap<String, Double>();
		ArrayList<String> keyIndicator = new ArrayList<String>();

		// Format Debt Date as per req.
		String formattedDate = Utility.formattedDate(dDate);

		// Get availabe scheme code for Debt
		String schemes = getAvailableDebtSchemes(formattedDate, data);

		// Calculate Debt for Asset/Security/KeyIndicator
		String[] dataLength = data.split(",");
		if(schemes != null) {
			assetQuality = getAssetQuality(schemes, formattedDate, dataLength.length);
			securityClass = getSecurityClass(schemes, formattedDate, dataLength.length);
			keyIndicator = getKeyIndicator(schemes, formattedDate, dataLength.length);
		}

		debtCal.put("AssetQuality", assetQuality);
		debtCal.put("SecurityClass", securityClass);
		debtCal.put("KeyIndicator", keyIndicator);

		return debtCal;
	}

	/**
	 * @param schemes
	 * @param formattedDate
	 * @param length
	 * @return debt key indicator data
	 */
	private ArrayList<String> getKeyIndicator(String schemes, String formattedDate, int length) {

		ArrayList<String> getKeyIndicator = new ArrayList<String>();

		String query = "select m.Cams_ProductCode as RTACode,sd.mduration as mduration,sd.avgtenor as avgtenor from crisilfa.scheme_details  sd "
				+ "inner join crisilfa.amfi_crisil_map m on m.crisilschemecode=sd.schemecode " + "where UPPER(TO_CHAR(sd.corpusdate,'MON-YYYY')) = '"
				+ formattedDate + "' and  " + "sd.schemecode in (" + schemes + ")";

		LOGGER.debug("Debt Key Indicator : " + query);

		return jdbcTemplate.query(query, new ResultSetExtractor<ArrayList<String>>() {

			@Override
			public ArrayList<String> extractData(ResultSet rs) throws SQLException, DataAccessException {

				while(rs.next()) {
					getKeyIndicator.add(rs.getString("RTACode") + "_" + rs.getDouble("mduration") + "_" + rs.getDouble("avgtenor"));
				}
				return getKeyIndicator;
			}

		});

	}

	/**
	 * @param schemes
	 * @param formattedDate
	 * @param length
	 * @return Debt Security Data
	 */
	@Override
	public LinkedHashMap<String, Double> getSecurityClass(String schemes, String formattedDate, int length) {

		LinkedHashMap<String, Double> securityClass = new LinkedHashMap<String, Double>();

		String query = "select sp.SecurityClass as SecurityClass,sum(sp.percenttonav)/" + length + " as Exposure from "
				+ "crisilfa.scheme_details  sd inner join crisilfa.scheme_portfolio sp on "
				+ "sd.portfolioblockid=sp.portfolioblockid and sp.SecurityClass is not null and  " + "sd.schemecode in ( " + schemes
				+ " ) and UPPER(TO_CHAR(sd.corpusdate,'MON-YYYY')) = '" + formattedDate + "' group by sp. SecurityClass";

		LOGGER.debug("Debt Security Class : " + query);

		return jdbcTemplate.query(query, new ResultSetExtractor<LinkedHashMap<String, Double>>() {

			@Override
			public LinkedHashMap<String, Double> extractData(ResultSet rs) throws SQLException, DataAccessException {

				while(rs.next()) {
					securityClass.put(rs.getString("SecurityClass"), rs.getDouble("Exposure"));
				}
				return securityClass;
			}

		});

	}

	/**
	 * @param schemes
	 * @param formattedDate
	 * @param length
	 * @return Debt Asset Data
	 */
	private LinkedHashMap<String, Double> getAssetQuality(String schemes, String formattedDate, int length) {

		LinkedHashMap<String, Double> assetQuality = new LinkedHashMap<String, Double>();

		String query = "select sp.Rating as rating,sum(sp.percenttonav)/" + length + " as Exposure from crisilfa.scheme_details sd "
				+ "inner join crisilfa.scheme_portfolio sp on sd.portfolioblockid=sp.portfolioblockid and "
				+ "sp.Rating is not null and  sd.schemecode in (" + schemes + ") and UPPER(TO_CHAR(sd.corpusdate,'MON-YYYY')) = '" + formattedDate
				+ "' group by sp.Rating";

		LOGGER.debug("Debt Asset Quality : " + query);

		return jdbcTemplate.query(query, new ResultSetExtractor<LinkedHashMap<String, Double>>() {

			@Override
			public LinkedHashMap<String, Double> extractData(ResultSet rs) throws SQLException, DataAccessException {

				while(rs.next()) {
					assetQuality.put(rs.getString("rating"), rs.getDouble("Exposure"));
				}
				return assetQuality;
			}

		});

	}

	/**
	 * @param formattedDate
	 * @param data
	 * @return Debt Scheme
	 */
	@Override
	public String getAvailableDebtSchemes(String formattedDate, String data) {

		String query = "select distinct sd.schemecode as schemecode from  crisilfa.scheme_details sd "
				+ "inner join crisilfa.amfi_crisil_map m on sd.schemecode=m.crisilschemecode " + "and m.cams_productcode in(" + data
				+ ") inner join crisilfa.scheme s on s.schemecode=sd.schemecode "
				+ "and s.iscorporate <> 'Y' inner join crisilfa.scheme_portfolio sp on " + "sd.portfolioblockid=sp.portfolioblockid  and "
				+ "UPPER(TO_CHAR(sp.portfoliodate,'MON-YYYY')) ='" + formattedDate + "'";

		LOGGER.debug("Available Debt Schemes : " + query);

		return jdbcTemplate.query(query, new ResultSetExtractor<String>() {
			String str = "";

			@Override
			public String extractData(ResultSet rs) throws SQLException, DataAccessException {

				while(rs.next()) {
					if(str.equals("")) {
						str = "'" + rs.getString("schemecode") + "'";
					} else {
						str = str + ",'" + rs.getString("schemecode") + "'";
					}
				}
				return str;
			}

		});

	}

	/**
	 * @param debtSchCode
	 * @return debt date
	 */
	@Override
	public Date getMinDateForDebt(String debtSchCode) {

		String query = "select min(cdate) as Pdate from (select sd.schemecode,max(sd.corpusdate)as Cdate from  "
				+ "crisilfa.scheme_details sd inner join crisilfa.scheme_portfolio sp on "
				+ "sd.portfolioblockid=sp.portfolioblockid  and  sd.schemecode in "
				+ "( Select crisilschemecode from crisilfa.amfi_crisil_map where cams_productcode" + " in(" + debtSchCode
				+ ")) group by sd.schemecode)";

		LOGGER.debug("Debt Date : " + query);

		return jdbcTemplate.query(query, new ResultSetExtractor<Date>() {
			Date d;

			@Override
			public Date extractData(ResultSet rs) throws SQLException, DataAccessException {

				while(rs.next()) {
					d = rs.getDate("Pdate");
				}
				return d;
			}

		});
	}

	/**
	 * @param schemes
	 * @param formattedDate
	 * @param length
	 * @return top five details
	 */
	private LinkedHashMap<String, Double> getDetailsOfTopFive(String schemes, String formattedDate, int length) {

		LinkedHashMap<String, Double> finalDataTopFive = new LinkedHashMap<String, Double>();

		String query = "select c.IndustryName as industryname,c.Exposure as exposure from (select sp.IndustryName,sum(sp.percenttonav)/" + length
				+ " as Exposure, " + "rank() over (order by sum(sp.percenttonav)/" + length + " desc) r from "
				+ "crisilfa.scheme_details  sd  inner join crisilfa.scheme_portfolio sp on "
				+ "sd.portfolioblockid=sp.portfolioblockid and sp.IndustryName is not null and  " + "sd.schemecode in (" + schemes
				+ ") and UPPER(TO_CHAR(sd.corpusdate,'MON-YYYY')) = '" + formattedDate + "' " + "group by sp.IndustryName) c  where r < 6";

		LOGGER.debug("Details Of Top Five : " + query);

		return jdbcTemplate.query(query, new ResultSetExtractor<LinkedHashMap<String, Double>>() {

			@Override
			public LinkedHashMap<String, Double> extractData(ResultSet rs) throws SQLException, DataAccessException {

				while(rs.next()) {
					finalDataTopFive.put(rs.getString("industryname"), rs.getDouble("exposure"));
				}
				return finalDataTopFive;
			}

		});

	}

	/**
	 * @param schemes
	 * @param formattedDate
	 * @param length
	 * @return top ten company details
	 */
	private LinkedHashMap<String, Double> getDetailsOfTopTen(String schemes, String formattedDate, int length) {

		LinkedHashMap<String, Double> finalDataTopTen = new LinkedHashMap<String, Double>();

		String query = "select c.CompanyName as cmpnyname,c.Exposure as exposure from (select sp.CompanyName,sum(sp.percenttonav)/" + length
				+ " as Exposure , " + "rank() over (order by sum(sp.percenttonav)/" + length + " desc) r from crisilfa.scheme_details  sd "
				+ "inner join crisilfa.scheme_portfolio sp on sd.portfolioblockid=sp.portfolioblockid "
				+ "and sp.CompanyName is not null and  sd.schemecode in (" + schemes + ") " + "and UPPER(TO_CHAR(sd.corpusdate,'MON-YYYY')) = '"
				+ formattedDate + "' " + "group by sp.CompanyName) c  where r < 11";

		LOGGER.debug("Details Of Top Ten : " + query);

		return jdbcTemplate.query(query, new ResultSetExtractor<LinkedHashMap<String, Double>>() {

			@Override
			public LinkedHashMap<String, Double> extractData(ResultSet rs) throws SQLException, DataAccessException {

				while(rs.next()) {
					finalDataTopTen.put(rs.getString("cmpnyname"), rs.getDouble("exposure"));
				}
				return finalDataTopTen;
			}

		});
	}

	/**
	 * @param formattedDate
	 * @param data
	 * @return SchCode
	 */
	private String getAvailableEquitySchemes(String formattedDate, String data) {
		String query = "select distinct sd.schemecode as schemecode from  "
				+ "crisilfa.scheme_details sd inner join crisilfa.amfi_crisil_map m on sd.schemecode=m.crisilschemecode "
				+ "and m.cams_productcode in(" + data + ") " + "inner join crisilfa.scheme s on s.schemecode=sd.schemecode "
				+ "and s.iscorporate <> 'Y' inner join crisilfa.scheme_portfolio sp on sd.portfolioblockid=sp.portfolioblockid "
				+ "and UPPER(TO_CHAR(sp.portfoliodate,'MON-YYYY')) ='" + formattedDate + "'";

		LOGGER.debug("Available Equity Schemes : " + query);

		return jdbcTemplate.query(query, new ResultSetExtractor<String>() {
			String str = "";

			@Override
			public String extractData(ResultSet rs) throws SQLException, DataAccessException {

				while(rs.next()) {
					if(str.equals("")) {
						str = "'" + rs.getString("schemecode") + "'";
					} else {
						str = str + ",'" + rs.getString("schemecode") + "'";
					}
				}
				return str;
			}

		});

	}

	/**
	 * Get Date
	 * 
	 * @param equitySchCode
	 * @return
	 */
	@Override
	public Date getMinDateForEquity(String equitySchCode) {
		String query = "select min(cdate) as Pdate from (select sd.schemecode,max(sd.corpusdate)as Cdate "
				+ "from  crisilfa.scheme_details sd inner join crisilfa.scheme_portfolio sp on sd.portfolioblockid=sp.portfolioblockid "
				+ "and  sd.schemecode in ( Select crisilschemecode from crisilfa.amfi_crisil_map " + "where cams_productcode in(" + equitySchCode
				+ ")) " + "group by sd.schemecode )";

		LOGGER.debug("MinDateForEquity : " + query);

		return jdbcTemplate.query(query, new ResultSetExtractor<Date>() {
			Date d;

			@Override
			public Date extractData(ResultSet rs) throws SQLException, DataAccessException {

				while(rs.next()) {
					d = rs.getDate("Pdate");
				}
				return d;
			}

		});

	}

	/**
	 * @param schemes
	 * @param formattedDate
	 * @param length
	 * @return Debt Security Data
	 */
	@Override
	public LinkedHashMap<String, Double> getSecurityClassForPie(String schemes, String formattedDate, int length) {

		LinkedHashMap<String, Double> securityClass = new LinkedHashMap<String, Double>();

		String query = "SELECT sClass as sClass,sum(percenttonav)/" + length + " as Exposure  FROM ( select ( case "
				+ " when UPPER(sp.SecurityClass) = 'CALL' then   'Call, CBLO, Reverse Repo, Net receivables' "
				+ " when UPPER(sp.SecurityClass) = 'CBLO' then   'Call, CBLO, Reverse Repo, Net receivables' "
				+ " when UPPER(sp.SecurityClass) = 'NET RECEIVABLES' then   'Call, CBLO, Reverse Repo, Net receivables' "
				+ " when UPPER(sp.SecurityClass) = 'REVERSE REPO' then  'Call, CBLO, Reverse Repo, Net receivables' "

				+ " when UPPER(sp.SecurityClass) = 'CURRENCY FUTURES' then  'Futures'"
				+ " when UPPER(sp.SecurityClass) = 'F\\&O(TYPENOTSPECIFIED)' then  'Futures'"
				+ " when UPPER(sp.SecurityClass) = 'FUTURES FAR' then  'Futures' " + " when UPPER(sp.SecurityClass) = 'FUTURES MID' then  'Futures' "
				+ " when UPPER(sp.SecurityClass) = 'FUTURES NEAR' then  'Futures'" + " when UPPER(sp.SecurityClass) = 'IRF' then  'Futures'"

				+ " when UPPER(sp.SecurityClass) = 'GOI' then   'GOI, SDL, T-Bills'"
				+ " when UPPER(sp.SecurityClass) = 'T-BILLS' then   'GOI, SDL, T-Bills'"

				+ " when UPPER(sp.SecurityClass) = 'FOREIGN EQUITY' then   'Foreign investments'"
				+ " when UPPER(sp.SecurityClass) = 'FOREIGN SECURITY' then  'Foreign investments'"

				+ " when UPPER(sp.SecurityClass) = 'NCD \\& BONDS' then   'NCD & Bonds'"
				+ " when UPPER(sp.SecurityClass) = 'NCD-ST' then   'NCD & Bonds'"

				+ " when UPPER(sp.SecurityClass) = 'ADR' then   'Others'" + " when UPPER(sp.SecurityClass) = 'BRDS' then    'Others'"
				+ " when UPPER(sp.SecurityClass) = 'CALL OPTIONS' then    'Others'" + " when UPPER(sp.SecurityClass) = 'PUT OPTIONS' then    'Others'"
				+ " when UPPER(sp.SecurityClass) = 'GDR' then    'Others'" + " when UPPER(sp.SecurityClass) = 'IDR' then    'Others'"
				+ " when UPPER(sp.SecurityClass) = 'INTEREST RATE SWAP' then   'Others'"
				+ " when UPPER(sp.SecurityClass) = 'MARGIN MONEY' then    'Others'" + " when UPPER(sp.SecurityClass) = 'MF UNITS' then    'Others'"
				+ " when UPPER(sp.SecurityClass) = 'MIBOR' then    'Others'" + " when UPPER(sp.SecurityClass) = 'PTC' then    'Others'"
				+ " when UPPER(sp.SecurityClass) = 'SG' then    'Others'"
				+ " when UPPER(sp.SecurityClass) = 'WARRANTS' then    'Others' else sp.SecurityClass end )as sClass,sp.percenttonav "
				+ " from crisilfa.scheme_details  sd "
				+ " inner join crisilfa.scheme_portfolio sp on sd.portfolioblockid=sp.portfolioblockid and sp.SecurityClass is not null"
				+ " and  sd.schemecode in ( " + schemes + ") and UPPER(TO_CHAR(sd.corpusdate,'MON-YYYY')) = '" + formattedDate
				+ "') x group by x.sClass";

		LOGGER.debug("Debt Security Class : " + query);

		return jdbcTemplate.query(query, new ResultSetExtractor<LinkedHashMap<String, Double>>() {

			@Override
			public LinkedHashMap<String, Double> extractData(ResultSet rs) throws SQLException, DataAccessException {

				while(rs.next()) {
					securityClass.put(rs.getString("sClass"), rs.getDouble("Exposure"));
				}
				return securityClass;
			}

		});

	}

}
