/**
 * 
 */
package vnet.sms.common.wme;

import java.io.Serializable;

import org.jboss.netty.channel.UpstreamMessageEvent;

import vnet.sms.common.messages.Sms;

/**
 * @author obergner
 * 
 */
public class SmsReceivedEvent<ID extends Serializable> extends
        UpstreamWindowedMessageEvent<ID, Sms> {

	public SmsReceivedEvent(final ID messageReference,
	        final UpstreamMessageEvent upstreamMessageEvent, final Sms sms) {
		super(messageReference, WindowedMessageEvent.Type.SMS_RECEIVED,
		        upstreamMessageEvent, sms);
	}
}