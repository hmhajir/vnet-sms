/**
 * 
 */
package vnet.sms.common.wme.send;

import static org.apache.commons.lang.Validate.notNull;

import java.io.Serializable;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.DownstreamMessageEvent;

import vnet.sms.common.messages.GsmPdu;
import vnet.sms.common.wme.MessageEventType;
import vnet.sms.common.wme.WindowedMessageEvent;

/**
 * @author obergner
 * 
 */
public abstract class DownstreamWindowedMessageEvent<ID extends Serializable, M extends GsmPdu>
        extends DownstreamMessageEvent implements WindowedMessageEvent<ID, M> {

	private final ID	           messageReference;

	private final MessageEventType	type;

	protected DownstreamWindowedMessageEvent(final ID messageReference,
	        final MessageEventType type,
	        final DownstreamMessageEvent downstreamMessageEvent, final M message) {
		this(messageReference, type, downstreamMessageEvent.getChannel(),
		        downstreamMessageEvent.getFuture(), message);
	}

	protected DownstreamWindowedMessageEvent(final ID messageReference,
	        final MessageEventType type, final Channel channel,
	        final ChannelFuture future, final Object message) {
		super(channel, future, message, channel.getRemoteAddress());
		notNull(messageReference,
		        "Argument 'messageReference' must not be null");
		notNull(channel, "Argument 'channel' must not be null");
		notNull(future, "Argument 'future' must not be null");
		notNull(message, "Argument 'message' must not be null");
		this.messageReference = messageReference;
		this.type = type;
	}

	/**
	 * @see vnet.sms.common.wme.WindowedMessageEvent#getMessageReference()
	 */
	@Override
	public ID getMessageReference() {
		return this.messageReference;
	}

	/**
	 * @see vnet.sms.common.wme.WindowedMessageEvent#getMessageType()
	 */
	@Override
	public MessageEventType getMessageType() {
		return this.type;
	}

	/**
	 * @see vnet.sms.common.wme.WindowedMessageEvent#getMessage()
	 */
	@Override
	public M getMessage() {
		return (M) super.getMessage();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
		        * result
		        + ((this.messageReference == null) ? 0 : this.messageReference
		                .hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final DownstreamWindowedMessageEvent<? extends Serializable, ? extends GsmPdu> other = (DownstreamWindowedMessageEvent<? extends Serializable, ? extends GsmPdu>) obj;
		if (this.messageReference == null) {
			if (other.messageReference != null) {
				return false;
			}
		} else if (!this.messageReference.equals(other.messageReference)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + hashCode()
		        + "[messageReference: " + this.messageReference + "|message: "
		        + getMessage() + "|channel: " + getChannel()
		        + "|remoteAddress: " + getRemoteAddress() + "]";
	}
}
