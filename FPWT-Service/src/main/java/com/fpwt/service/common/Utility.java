package com.fpwt.service.common;

import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;

/**
 * @author c-NikunjP
 *
 */
public class Utility {

	/**
	 * return date in "MM-YYYY"
	 * @param minEquityDate
	 * @return
	 */
	public static String formattedDate(Date minEquityDate) {
		String str = null;
		if (minEquityDate != null) {
			Formatter fmt = new Formatter();
			Calendar cal = Calendar.getInstance();
			cal.setTime(minEquityDate);
			fmt = new Formatter();
			fmt.format("%tb", minEquityDate);
			str = fmt.toString().toUpperCase() + "-" + cal.get(Calendar.YEAR);
		}
		return str;
	}
}
