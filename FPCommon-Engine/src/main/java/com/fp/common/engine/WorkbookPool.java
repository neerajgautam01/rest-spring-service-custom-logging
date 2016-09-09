package com.fp.common.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.aspose.cells.Workbook;

public class WorkbookPool {

	public static Map<String, List<Workbook>> wbPool = new HashMap<String, List<Workbook>>();
	public static long poolSize = 20;

	private static Logger LOGGER = Logger.getLogger(WorkbookPool.class.getName());

	public static List<Workbook> generatePoolOfWB(String fileName) {
		List<Workbook> workbookList = new ArrayList<Workbook>();
		try {
			for(int i = 0; i < poolSize; i++) {
				Workbook wb = new Workbook(fileName);
				wb.calculateFormula();
				workbookList.add(wb);
			}

		}
		catch(Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return workbookList;
	}

	public static Workbook getWB(String fileName, String version) {
		Workbook temp = null;
		if(!wbPool.containsKey(fileName + version))
			return null;
		if(wbPool.get(fileName + version).size() > 0) {
			temp = wbPool.get(fileName + version).remove(0);
			LOGGER.info(fileName + version + " pool size = " + wbPool.get(fileName + version).size());
			return temp;
		} else
			return null;
	}

	public static void addWB(String fileName, String version, Workbook wb) {

		if(wbPool.containsKey(fileName + version)) {
			wbPool.get(fileName + version).add(wb);
			LOGGER.info("Wb returned to pool.");
		}
	}

}
