package vnet.sms.common.shell.springshell.internal.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jline.ANSIBuffer;
import jline.ConsoleReader;
import vnet.sms.common.shell.springshell.ShellPromptAccessor;
import vnet.sms.common.shell.springshell.internal.util.Assert;
import vnet.sms.common.shell.springshell.internal.util.IOUtils;
import vnet.sms.common.shell.springshell.internal.util.OsUtils;
import vnet.sms.common.shell.springshell.internal.util.StringUtils;

/**
 * JDK logging {@link Handler} that emits log messages to a JLine
 * {@link ConsoleReader}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class JLineLogHandler extends Handler {

	// Constants
	private static final boolean	    BRIGHT_COLORS	          = Boolean
	                                                                      .getBoolean("roo.bright");

	private static ThreadLocal<Boolean>	redrawProhibit	          = new ThreadLocal<Boolean>();

	private static String	            lastMessage;

	private static boolean	            includeThreadName	      = false;

	private static boolean	            suppressDuplicateMessages	= true;

	// Fields
	private final ConsoleReader	        reader;

	private final ShellPromptAccessor	shellPromptAccessor;

	private final boolean	            ansiSupported;

	private final String	            userInterfaceThreadName;

	public JLineLogHandler(final ConsoleReader reader,
	        final ShellPromptAccessor shellPromptAccessor) {
		Assert.notNull(reader, "Console reader required");
		Assert.notNull(shellPromptAccessor, "Shell prompt accessor required");
		this.reader = reader;
		this.shellPromptAccessor = shellPromptAccessor;
		this.userInterfaceThreadName = Thread.currentThread().getName();
		this.ansiSupported = reader.getTerminal().isANSISupported();

		setFormatter(new Formatter() {
			@Override
			public String format(final LogRecord record) {
				final StringBuffer sb = new StringBuffer();
				if (record.getMessage() != null) {
					sb.append(record.getMessage()).append(
					        StringUtils.LINE_SEPARATOR);
				}
				if (record.getThrown() != null) {
					PrintWriter pw = null;
					try {
						final StringWriter sw = new StringWriter();
						pw = new PrintWriter(sw);
						record.getThrown().printStackTrace(pw);
						sb.append(sw.toString());
					} catch (final Exception ex) {
					} finally {
						IOUtils.closeQuietly(pw);
					}
				}
				return sb.toString();
			}
		});
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() throws SecurityException {
	}

	public static void prohibitRedraw() {
		redrawProhibit.set(true);
	}

	public static void cancelRedrawProhibition() {
		redrawProhibit.remove();
	}

	public static void setIncludeThreadName(final boolean include) {
		includeThreadName = include;
	}

	public static void resetMessageTracking() {
		lastMessage = null; // see ROO-251
	}

	public static boolean isSuppressDuplicateMessages() {
		return suppressDuplicateMessages;
	}

	public static void setSuppressDuplicateMessages(
	        final boolean suppressDuplicateMessages) {
		JLineLogHandler.suppressDuplicateMessages = suppressDuplicateMessages;
	}

	@Override
	public void publish(final LogRecord record) {
		try {
			// Avoid repeating the same message that displayed immediately
			// before the current message (ROO-30, ROO-1873)
			final String toDisplay = toDisplay(record);
			if (toDisplay.equals(lastMessage) && suppressDuplicateMessages) {
				return;
			}
			lastMessage = toDisplay;

			final StringBuffer buffer = this.reader.getCursorBuffer()
			        .getBuffer();
			final int cursor = this.reader.getCursorBuffer().cursor;
			if (this.reader.getCursorBuffer().length() > 0) {
				// The user has semi-typed something, so put a new line in so
				// the debug message is separated
				this.reader.printNewline();

				// We need to cancel whatever they typed (it's reset later on),
				// so the line appears empty
				this.reader.getCursorBuffer().setBuffer(new StringBuffer());
				this.reader.getCursorBuffer().cursor = 0;
			}

			// This ensures nothing is ever displayed when redrawing the line
			this.reader.setDefaultPrompt("");
			this.reader.printString(toDisplay);

			// Now restore the line formatting settings back to their original
			this.reader.setDefaultPrompt(this.shellPromptAccessor
			        .getShellPrompt());

			this.reader.getCursorBuffer().setBuffer(buffer);
			this.reader.getCursorBuffer().cursor = cursor;

			final Boolean prohibitingRedraw = redrawProhibit.get();
			if (prohibitingRedraw == null) {
				this.reader.redrawLine();
			}

			this.reader.flushConsole();
		} catch (final Exception e) {
			reportError("Could not publish log message", e,
			        Level.SEVERE.intValue());
		}
	}

	private String toDisplay(final LogRecord event) {
		final StringBuilder sb = new StringBuilder();

		String threadName;
		String eventString;
		if (includeThreadName
		        && !this.userInterfaceThreadName.equals(Thread.currentThread()
		                .getName())
		        && !"".equals(Thread.currentThread().getName())) {
			threadName = "[" + Thread.currentThread().getName() + "]";

			// Build an event string that will indent nicely given the left hand
			// side now contains a thread name
			final StringBuilder lineSeparatorAndIndentingString = new StringBuilder();
			for (int i = 0; i <= threadName.length(); i++) {
				lineSeparatorAndIndentingString.append(" ");
			}

			eventString = " "
			        + getFormatter().format(event).replace(
			                StringUtils.LINE_SEPARATOR,
			                StringUtils.LINE_SEPARATOR
			                        + lineSeparatorAndIndentingString
			                                .toString());
			if (eventString
			        .endsWith(lineSeparatorAndIndentingString.toString())) {
				eventString = eventString.substring(0, eventString.length()
				        - lineSeparatorAndIndentingString.length());
			}
		} else {
			threadName = "";
			eventString = getFormatter().format(event);
		}

		if (this.ansiSupported) {
			if (event.getLevel().intValue() >= Level.SEVERE.intValue()) {
				sb.append(getANSIBuffer().reverse(threadName).red(eventString));
			} else if (event.getLevel().intValue() >= Level.WARNING.intValue()) {
				sb.append(getANSIBuffer().reverse(threadName).magenta(
				        eventString));
			} else if (event.getLevel().intValue() >= Level.INFO.intValue()) {
				sb.append(getANSIBuffer().reverse(threadName)
				        .green(eventString));
			} else {
				sb.append(getANSIBuffer().reverse(threadName).append(
				        eventString));
			}
		} else {
			sb.append(threadName).append(eventString);
		}

		return sb.toString();
	}

	/**
	 * Makes text brighter if requested through system property 'roo.bright' and
	 * works around issue on Windows in using reverse() in combination with the
	 * Jansi lib, which leaves its 'negative' flag set unless reset explicitly.
	 * 
	 * @return new patched ANSIBuffer
	 */
	public static ANSIBuffer getANSIBuffer() {
		final char esc = (char) 27;
		return new ANSIBuffer() {
			@Override
			public ANSIBuffer reverse(final String str) {
				if (OsUtils.isWindows()) {
					return super.reverse(str).append(ANSICodes.attrib(esc));
				}
				return super.reverse(str);
			};

			@Override
			public ANSIBuffer attrib(final String str, final int code) {
				if (BRIGHT_COLORS && (30 <= code) && (code <= 37)) {
					// This is a color code: add a 'bright' code
					return append(esc + "[" + code + ";1m").append(str).append(
					        ANSICodes.attrib(0));
				}
				return super.attrib(str, code);
			}
		};
	}
}
