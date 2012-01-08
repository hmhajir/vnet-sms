package vnet.sms.gateway.server.framework;

import static org.easymock.EasyMock.createNiceMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;

import org.jboss.netty.handler.codec.serialization.ClassResolvers;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.junit.Test;
import org.springframework.jms.core.JmsTemplate;

import vnet.sms.common.wme.jmsbridge.WindowedMessageEventToJmsMessageConverter;
import vnet.sms.gateway.nettysupport.monitor.ChannelMonitorRegistry;
import vnet.sms.gateway.server.framework.channel.GatewayServerChannelPipelineFactory;
import vnet.sms.gateway.server.framework.jmsbridge.MessageForwardingJmsBridge;
import vnet.sms.gateway.server.framework.test.DenyAllAuthenticationManager;
import vnet.sms.gateway.server.framework.test.SerialIntegersMessageReferenceGenerator;
import vnet.sms.gateway.transports.serialization.ReferenceableMessageContainer;
import vnet.sms.gateway.transports.serialization.incoming.SerializationTransportProtocolAdaptingUpstreamChannelHandler;
import vnet.sms.gateway.transports.serialization.outgoing.SerializationTransportProtocolAdaptingDownstreamChannelHandler;

import com.mockrunner.jms.ConfigurationManager;
import com.mockrunner.jms.DestinationManager;
import com.mockrunner.mock.jms.MockConnectionFactory;

public class GatewayServerBuilderTest {

	@Test(expected = IllegalStateException.class)
	public final void assertThatAfterPropertiesSetRefusesToBeCalledTwice()
	        throws Exception {
		final GatewayServerBuilder<Integer, ReferenceableMessageContainer> objectUnderTest = new GatewayServerBuilder<Integer, ReferenceableMessageContainer>();
		objectUnderTest
		        .setInstanceId("assertThatAfterPropertiesSetRefusesToBeCalledTwice");
		objectUnderTest.setPort(1000);
		objectUnderTest
		        .setChannelPipelineFactory(createNiceMock(GatewayServerChannelPipelineFactory.class));

		objectUnderTest.afterPropertiesSet();
		objectUnderTest.afterPropertiesSet();
	}

	@Test(expected = IllegalStateException.class)
	public final void assertThatGetObjectRecognizesThatAfterPropertiesSetHasNotBeenCalled()
	        throws Exception {
		final GatewayServerBuilder<Integer, ReferenceableMessageContainer> objectUnderTest = new GatewayServerBuilder<Integer, ReferenceableMessageContainer>();
		objectUnderTest
		        .setInstanceId("assertThatGetObjectRecognizesThatAfterPropertiesSetHasNotBeenCalled");
		objectUnderTest.setPort(1000);
		objectUnderTest
		        .setChannelPipelineFactory(createNiceMock(GatewayServerChannelPipelineFactory.class));

		objectUnderTest.getObject();
	}

	@Test
	public final void assertThatGetObjectReturnsProperGatewayServerAsSoonAsAfterPropertiesSetHasBeenCalled()
	        throws Exception {
		final GatewayServerBuilder<Integer, ReferenceableMessageContainer> objectUnderTest = new GatewayServerBuilder<Integer, ReferenceableMessageContainer>();
		objectUnderTest
		        .setInstanceId("assertThatAfterPropertiesSetRefusesToBeCalledTwice");
		objectUnderTest.setPort(1000);
		objectUnderTest
		        .setChannelPipelineFactory(createNiceMock(GatewayServerChannelPipelineFactory.class));

		objectUnderTest.afterPropertiesSet();

		final GatewayServer<Integer, ReferenceableMessageContainer> product = objectUnderTest
		        .getObject();

		assertNotNull(
		        "getObject() returned null although GatewayServer should have been built in afterPropertiesSet()",
		        product);
	}

	@Test
	public final void assertThatGetObjectTypeReturnsSubtypeOfGatewayServer() {
		final GatewayServerBuilder<Integer, ReferenceableMessageContainer> objectUnderTest = new GatewayServerBuilder<Integer, ReferenceableMessageContainer>();

		final Class<?> objectType = objectUnderTest.getObjectType();

		assertTrue(
		        "getObjectType() should have returned a subtype of GatewayServer, yet it didn't",
		        GatewayServer.class.isAssignableFrom(objectType));
	}

	@Test
	public final void assertThatIsSingletonReturnsTrue() {
		final GatewayServerBuilder<Integer, ReferenceableMessageContainer> objectUnderTest = new GatewayServerBuilder<Integer, ReferenceableMessageContainer>();

		assertTrue("isSingleton() should have returned true",
		        objectUnderTest.isSingleton());
	}

	@Test
	public final void assertThatDestroyStopsTheCreatedGatewayServer()
	        throws Exception {
		final GatewayServerBuilder<Integer, ReferenceableMessageContainer> objectUnderTest = new GatewayServerBuilder<Integer, ReferenceableMessageContainer>();
		objectUnderTest
		        .setInstanceId("assertThatDestroyStopsTheCreatedGatewayServer");
		objectUnderTest.setPort(65500);
		objectUnderTest
		        .setChannelPipelineFactory(newGatewayServerChannelPipelineFactory());
		objectUnderTest.afterPropertiesSet();

		final GatewayServer<Integer, ReferenceableMessageContainer> product = objectUnderTest
		        .getObject();
		product.start();

		objectUnderTest.destroy();

		assertEquals(
		        "destroy() should have stopped the created GatewayServer, yet it didn't",
		        ServerStatus.STOPPED, product.getCurrentStatus());
	}

	private GatewayServerChannelPipelineFactory<Integer, ReferenceableMessageContainer> newGatewayServerChannelPipelineFactory() {
		return new GatewayServerChannelPipelineFactory<Integer, ReferenceableMessageContainer>(
		        "newObjectUnderTest",
		        ReferenceableMessageContainer.class,
		        new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
		        null,
		        new ObjectEncoder(),
		        new SerializationTransportProtocolAdaptingUpstreamChannelHandler(),
		        new SerializationTransportProtocolAdaptingDownstreamChannelHandler(),
		        new ChannelMonitorRegistry(),
		        new MessageForwardingJmsBridge<Integer>(newJmsTemplate()), 100,
		        10000, new DenyAllAuthenticationManager(), 10000,
		        new SerialIntegersMessageReferenceGenerator(), 100, 20000,
		        ManagementFactory.getPlatformMBeanServer());
	}

	private final JmsTemplate newJmsTemplate() {
		final DestinationManager destinationManager = new DestinationManager();
		destinationManager.createQueue("default.queue");

		final ConfigurationManager configurationManager = new ConfigurationManager();

		final MockConnectionFactory mockConnectionFactory = new MockConnectionFactory(
		        destinationManager, configurationManager);

		final JmsTemplate jmsTemplate = new JmsTemplate(mockConnectionFactory);
		jmsTemplate
		        .setMessageConverter(new WindowedMessageEventToJmsMessageConverter());
		jmsTemplate.setDefaultDestinationName("default.queue");

		return jmsTemplate;
	}
}
