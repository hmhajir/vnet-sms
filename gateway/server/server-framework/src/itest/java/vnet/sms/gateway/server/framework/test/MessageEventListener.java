package vnet.sms.gateway.server.framework.test;

import org.jboss.netty.channel.MessageEvent;

public interface MessageEventListener {

	void messageEventReceived(MessageEvent e);
}
