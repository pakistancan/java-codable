/**
 * 
 */
package com.alix.codable.logging;

import java.util.HashMap;
import java.util.Map;

/**
 * @author muhammadali
 *
 */
// Don't want to add full logger at this time, want to keep it light weight, will add log4j in future if required
public class LogFactory {

	private LogFactory() {

	}

	private static LogFactory sharedInstance = new LogFactory();

	public static LogFactory getInstance() {
		return sharedInstance;
	}

	private Map<String, Logger> loggers = new HashMap<>();

	{
		loggers.put("default", new ConsoleLogger());
	}

	public Logger getLogger() {
		return new ConsoleLogger();
	}

}
