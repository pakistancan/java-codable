/**
 * 
 */
package io.github.pakistancan.codable.logging;

/**
 * @author muhammadali
 *
 */
public interface Logger {
	
	public void debug(String message);
	public void info(String message);
	public void warn(String message);
	public void error(String message);
}
