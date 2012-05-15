package vnet.sms.common.shell.springshell.command;

import vnet.sms.common.shell.springshell.Parser;
import vnet.sms.common.shell.springshell.internal.parser.SimpleParser;

/**
 * Utility methods relating to shell simple parser contexts.
 */
public final class CliSimpleParserContext {

	// Class fields
	private static ThreadLocal<SimpleParser>	simpleParserContextHolder	= new ThreadLocal<SimpleParser>();

	public static Parser getSimpleParserContext() {
		return simpleParserContextHolder.get();
	}

	public static void setSimpleParserContext(
	        final SimpleParser simpleParserContext) {
		simpleParserContextHolder.set(simpleParserContext);
	}

	public static void resetSimpleParserContext() {
		simpleParserContextHolder.remove();
	}

	/**
	 * Constructor is private to prevent instantiation
	 */
	private CliSimpleParserContext() {
	}
}
