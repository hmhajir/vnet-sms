package vnet.sms.gateway.nettysupport.monitor.outgoing;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Test;

import vnet.sms.common.messages.Message;
import vnet.sms.common.messages.PingRequest;
import vnet.sms.gateway.nettysupport.monitor.DefaultChannelMonitorCallback;
import vnet.sms.gateway.nettysupport.monitor.TestChannelMonitorRegistry;
import vnet.sms.gateway.nettytest.ChannelPipelineEmbedder;
import vnet.sms.gateway.nettytest.DefaultChannelPipelineEmbedder;

public class OutgoingPdusCountingChannelHandlerTest {

	private static class SimpleChannelMonitorCallback extends
	        DefaultChannelMonitorCallback {

		final AtomicLong	numberOfSentPdus	= new AtomicLong(0);

		void reset() {
			this.numberOfSentPdus.set(0);
		}

		@Override
		public void sendPdu() {
			this.numberOfSentPdus.incrementAndGet();
		}
	}

	private final SimpleChannelMonitorCallback	              monitorCallback	= new SimpleChannelMonitorCallback();

	private final OutgoingPdusCountingChannelHandler<Message>	objectUnderTest	= new OutgoingPdusCountingChannelHandler<Message>(
	                                                                                    new TestChannelMonitorRegistry(
	                                                                                            this.monitorCallback),
	                                                                                    Message.class);

	@After
	public void resetMonitor() {
		this.monitorCallback.reset();
	}

	@Test
	public final void assertThatWriteRequestedCorrectlyUpdatesNumberOfSentPdus()
	        throws Throwable {
		final int numberOfSentPdus = 32;
		final ChannelPipelineEmbedder embeddedPipeline = new DefaultChannelPipelineEmbedder(
		        this.objectUnderTest);

		for (int i = 0; i < numberOfSentPdus; i++) {
			embeddedPipeline.send(new PingRequest(new InetSocketAddress(1),
			        new InetSocketAddress(1)));
		}

		assertEquals(
		        "OutgoingPdusCountingChannelHandler did not correctly count number of sent PDUs",
		        numberOfSentPdus, this.monitorCallback.numberOfSentPdus.get());
	}
}