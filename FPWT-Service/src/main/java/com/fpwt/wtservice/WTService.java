/**
 * 
 */
package com.fpwt.wtservice;

import java.io.IOException;

import com.fpwt.service.beans.WTRequest;

/**
 * @author c-NikunjP
 *
 */
public interface WTService {

	/**
	 * Create WT PDF
	 * @param wtRequest
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	byte[] createWTPdf(WTRequest wtRequest) throws IOException, Exception;

}
