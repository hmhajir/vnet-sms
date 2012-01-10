package vnet.sms.gateway.server.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.jms.ObjectMessage;

import org.jboss.netty.channel.local.DefaultLocalServerChannelFactory;
import org.jboss.netty.channel.local.LocalAddress;
import org.junit.Test;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.authentication.AuthenticationManager;

import vnet.sms.common.messages.Sms;
import vnet.sms.gateway.server.framework.channel.GatewayServerChannelPipelineFactory;
import vnet.sms.gateway.server.framework.test.AcceptAllAuthenticationManager;
import vnet.sms.gateway.server.framework.test.LocalClient;
import vnet.sms.gateway.transports.serialization.ReferenceableMessageContainer;

public class SmsTest extends AbstractGatewayServerTest {

	@Test
	public final void assertThatGatewayServerForwardsReceivedSmsToJmsServer()
	        throws Throwable {
		final LocalAddress serverAddress = new LocalAddress("test:server:10");
		final LocalAddress clientAddress = new LocalAddress("test:client:10");

		final int availableIncomingWindows = 1000;
		final long incomingWindowWaitTimeMillis = 1L;
		final long failedLoginResponseMillis = 100L;
		final int pingIntervalSeconds = 1;
		final long pingResponseTimeoutMillis = 200000L;
		final AuthenticationManager authenticationManager = new AcceptAllAuthenticationManager();
		final JmsTemplate jmsTemplate = newJmsTemplate();
		final GatewayServerChannelPipelineFactory<Integer, ReferenceableMessageContainer> channelPipelineFactory = newGatewayServerChannelPipelineFactory(
		        "assertThatGatewayServerForwardsReceivedSmsToJmsServer",
		        availableIncomingWindows, incomingWindowWaitTimeMillis,
		        failedLoginResponseMillis, pingIntervalSeconds,
		        pingResponseTimeoutMillis, authenticationManager, jmsTemplate);

		final GatewayServer<Integer, ReferenceableMessageContainer> objectUnderTest = new GatewayServer<Integer, ReferenceableMessageContainer>(
		        "assertThatGatewayServerForwardsReceivedSmsToJmsServer",
		        serverAddress, new DefaultLocalServerChannelFactory(),
		        channelPipelineFactory);
		objectUnderTest.start();

		final LocalClient client = new LocalClient(serverAddress);
		// Should start ping timeout
		client.connect();

		// Login. Otherwise, our LoginResponse will be discarded
		client.login(1,
		        "assertThatGatewayServerForwardsReceivedSmsToJmsServer",
		        "whatever");
		// Discard forwarded LoginRequest
		jmsTemplate.receive();

		final Sms sms = new Sms(
		        "assertThatGatewayServerForwardsReceivedSmsToJmsServer",
		        clientAddress, serverAddress);
		client.sendMessage(2, sms);
		client.disconnect();
		final ObjectMessage forwardedMessage = (ObjectMessage) jmsTemplate
		        .receive();
		objectUnderTest.stop();

		assertNotNull(
		        "GatewayServer should have forwarded SMS passed in to JMS server",
		        forwardedMessage);
		assertEquals(
		        "GatewayServer should have forwarded SMS passed in to JMS server",
		        sms, forwardedMessage.getObject());
	}
}