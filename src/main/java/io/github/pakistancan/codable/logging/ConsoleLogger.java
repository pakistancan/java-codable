/**
 * 
 */
package io.github.pakistancan.codable.logging;

/**
 * @author muhammadali
 *
 */
public class ConsoleLogger implements Logger {

	@Override
	public void debug(String message) {
		System.out.println(message);
	}

	@Override
	public void info(String message) {
		System.out.println(message);
	}

	@Override
	public void warn(String message) {
		System.out.println(message);
	}

	@Override
	public void error(String message) {
		System.out.println(message);
	}

}
