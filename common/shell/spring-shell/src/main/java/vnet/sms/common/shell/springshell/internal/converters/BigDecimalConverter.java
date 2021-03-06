package vnet.sms.common.shell.springshell.internal.converters;

import java.math.BigDecimal;
import java.util.List;

import vnet.sms.common.shell.springshell.Completion;
import vnet.sms.common.shell.springshell.Converter;
import vnet.sms.common.shell.springshell.MethodTarget;

/**
 * {@link Converter} for {@link BigDecimal}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public class BigDecimalConverter implements Converter<BigDecimal> {

	@Override
	public BigDecimal convertFromText(final String value,
	        final Class<?> requiredType, final String optionContext) {
		return new BigDecimal(value);
	}

	@Override
	public boolean getAllPossibleValues(final List<Completion> completions,
	        final Class<?> requiredType, final String existingData,
	        final String optionContext, final MethodTarget target) {
		return false;
	}

	@Override
	public boolean supports(final Class<?> requiredType,
	        final String optionContext) {
		return BigDecimal.class.isAssignableFrom(requiredType);
	}
}
