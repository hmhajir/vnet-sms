package vnet.sms.transports.serialization.outgoing;

import vnet.sms.gateway.nettysupport.LoginRequestAcceptedEvent;
import vnet.sms.gateway.nettysupport.LoginRequestRejectedEvent;
import vnet.sms.gateway.nettysupport.SendPingRequestEvent;
import vnet.sms.gateway.nettysupport.login.incoming.NonLoginMessageReceivedOnUnauthenticatedChannelEvent;
import vnet.sms.gateway.nettysupport.transport.outgoing.TransportProtocolAdaptingDownstreamChannelHandler;
import vnet.sms.transports.serialization.ReferenceableMessageContainer;

public class SerializationTransportProtocolAdaptingDownstreamChannelHandler
        extends
        TransportProtocolAdaptingDownstreamChannelHandler<Integer, ReferenceableMessageContainer> {

	@Override
	protected ReferenceableMessageContainer convertSendPingRequestEventToPdu(
	        final SendPingRequestEvent<Integer> e) {
		return ReferenceableMessageContainer.wrap(e.getMessageReference(),
		        e.getMessage());
	}

	@Override
	protected ReferenceableMessageContainer convertLoginRequestAcceptedEventToPdu(
	        final LoginRequestAcceptedEvent<Integer> e) {
		return ReferenceableMessageContainer.wrap(e.getMessageReference(),
		        e.getMessage());
	}

	@Override
	protected ReferenceableMessageContainer convertLoginRequestRejectedEventToPdu(
	        final LoginRequestRejectedEvent<Integer> e) {
		return ReferenceableMessageContainer.wrap(e.getMessageReference(),
		        e.getMessage());
	}

	@Override
	protected ReferenceableMessageContainer convertNonLoginMessageReceivedOnUnauthenticatedChannelEventToPdu(
	        final NonLoginMessageReceivedOnUnauthenticatedChannelEvent<Integer, ?> e) {
		return ReferenceableMessageContainer.wrap(e.getMessageReference(),
		        e.getMessage());
	}
}