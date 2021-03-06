/**
 * 
 */
package vnet.sms.gateway.nettysupport.login.incoming;

import static org.apache.commons.lang.Validate.notNull;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import vnet.sms.common.messages.GsmPdu;
import vnet.sms.common.wme.WindowedMessageEvent;
import vnet.sms.common.wme.acknowledge.SendLoginRequestAckEvent;
import vnet.sms.common.wme.acknowledge.SendLoginRequestNackEvent;
import vnet.sms.common.wme.receive.ReceivedLoginRequestEvent;
import vnet.sms.gateway.nettysupport.MessageProcessingContext;

/**
 * @author obergner
 * 
 */
public class IncomingLoginRequestsChannelHandler<ID extends Serializable>
        extends SimpleChannelUpstreamHandler {

	public static final String	                  NAME	                = "vnet.sms.gateway:incoming-login-handler";

	private final Logger	                      log	                = LoggerFactory
	                                                                            .getLogger(getClass());

	private final AuthenticationManager	          authenticationManager;

	private final AtomicReference<Authentication>	authenticatedClient	= new AtomicReference<Authentication>();

	private final long	                          failedLoginResponseDelayMillis;

	private final Timer	                          failedLoginResponseTimer;

	public IncomingLoginRequestsChannelHandler(
	        final AuthenticationManager authenticationManager,
	        final long failedLoginResponseDelayMillis,
	        final Timer failedLoginResponseTimer) {
		notNull(authenticationManager,
		        "Argument 'authenticationManager' must not be null");
		notNull(failedLoginResponseTimer,
		        "Argument 'failedLoginResponseTimer' must not be null");
		this.authenticationManager = authenticationManager;
		this.failedLoginResponseDelayMillis = failedLoginResponseDelayMillis;
		this.failedLoginResponseTimer = failedLoginResponseTimer;
	}

	/**
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext,
	 *      org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void messageReceived(final ChannelHandlerContext ctx,
	        final MessageEvent e) throws Exception {
		if (e instanceof ReceivedLoginRequestEvent) {
			loginRequestReceived(ctx, (ReceivedLoginRequestEvent<ID>) e);
		} else if (e instanceof WindowedMessageEvent) {
			nonLoginRequestReceived(ctx,
			        (WindowedMessageEvent<ID, ? extends GsmPdu>) e);
		} else {
			throw new IllegalArgumentException("Unsupported MessageEvent: " + e);
		}
	}

	private void loginRequestReceived(final ChannelHandlerContext ctx,
	        final ReceivedLoginRequestEvent<ID> e) {
		this.log.info(
		        "Attempting to authenticate current channel {} using credentials from {} ...",
		        ctx.getChannel(), e);
		if (isCurrentChannelAuthenticated()) {
			this.log.warn(
			        "Ignoring attempt to re-authenticate an already authenticated channel {}",
			        ctx.getChannel());
			return;
		}

		try {
			final Authentication authentication = this.authenticationManager
			        .authenticate(new UsernamePasswordAuthenticationToken(e
			                .getMessage().getUsername(), e.getMessage()
			                .getPassword()));
			processSuccessfulAuthentication(ctx, e, authentication);
		} catch (final AuthenticationException ae) {
			processFailedAuthentication(ctx, e, ae);
		}
		// Send LoginRequest further upstream - it might be needed for auditing,
		// logging, metrics ...
		ctx.sendUpstream(e);
	}

	private void processSuccessfulAuthentication(
	        final ChannelHandlerContext ctx,
	        final ReceivedLoginRequestEvent<ID> e,
	        final Authentication authentication) {
		if (!this.authenticatedClient.compareAndSet(null, authentication)) {
			this.log.warn(
			        "Ignoring attempt to re-authenticate an already authenticated channel {}",
			        ctx.getChannel());
			return;
		}

		try {
			MessageProcessingContext.INSTANCE.onUserEnter(authentication);
			this.log.info(
			        "Successfully authenticated channel {} - authenticated user is {}",
			        ctx.getChannel(), authentication.getPrincipal());
			ctx.sendDownstream(SendLoginRequestAckEvent.accept(e));
			// Inform the wider community ...
			ctx.sendUpstream(new ChannelSuccessfullyAuthenticatedEvent(ctx
			        .getChannel(), e.getMessage()));
		} finally {
			MessageProcessingContext.INSTANCE.onUserExit(authentication);
		}
	}

	private void processFailedAuthentication(final ChannelHandlerContext ctx,
	        final ReceivedLoginRequestEvent<ID> e,
	        final AuthenticationException ae) {
		this.log.warn("Authentication using credentials from " + e
		        + " failed - will delay negative response for ["
		        + this.failedLoginResponseDelayMillis
		        + "] milliseconds to prevent DoS attacks", ae);
		this.failedLoginResponseTimer.newTimeout(
		        this.new DelayFailedLoginResponse(ctx, e),
		        this.failedLoginResponseDelayMillis, TimeUnit.MILLISECONDS);
		// Inform the wider community ...
		ctx.sendUpstream(new ChannelAuthenticationFailedEvent(ctx.getChannel(),
		        e.getMessage(), ae));
	}

	private void nonLoginRequestReceived(final ChannelHandlerContext ctx,
	        final WindowedMessageEvent<ID, ? extends GsmPdu> e) {
		if (!isCurrentChannelAuthenticated()) {
			nonLoginRequestReceivedOnUnauthenticatedChannel(ctx, e);
			return;
		}

		nonLoginRequestReceivedOnAuthenticatedChannel(ctx, e);
	}

	private void nonLoginRequestReceivedOnUnauthenticatedChannel(
	        final ChannelHandlerContext ctx,
	        final WindowedMessageEvent<ID, ? extends GsmPdu> e) {
		this.log.warn(
		        "Received non-login request {} on UNAUTHENTICATED channel {} - DISCARD",
		        e, ctx.getChannel());
		ctx.sendDownstream(NonLoginMessageReceivedOnUnauthenticatedChannelEvent
		        .discardNonLoginMessage(e));
		return;
	}

	private void nonLoginRequestReceivedOnAuthenticatedChannel(
	        final ChannelHandlerContext ctx,
	        final WindowedMessageEvent<ID, ? extends GsmPdu> e)
	        throws IllegalArgumentException {
		try {
			MessageProcessingContext.INSTANCE
			        .onUserEnter(this.authenticatedClient.get());
			this.log.trace(
			        "Received non-login request {} on authenticated channel {} - will propagate event further upstream",
			        e, ctx.getChannel());
			ctx.sendUpstream(e);
		} finally {
			MessageProcessingContext.INSTANCE
			        .onUserExit(this.authenticatedClient.get());
		}
	}

	private boolean isCurrentChannelAuthenticated() {
		return this.authenticatedClient.get() != null;
	}

	private final class DelayFailedLoginResponse implements TimerTask {

		private final ChannelHandlerContext		    ctx;

		private final ReceivedLoginRequestEvent<ID>	rejectedLogin;

		DelayFailedLoginResponse(final ChannelHandlerContext ctx,
		        final ReceivedLoginRequestEvent<ID> rejectedLogin) {
			this.ctx = ctx;
			this.rejectedLogin = rejectedLogin;
		}

		@Override
		public void run(final Timeout timeout) throws Exception {
			if (timeout.isCancelled() || !this.ctx.getChannel().isOpen()) {
				return;
			}
			IncomingLoginRequestsChannelHandler.this.log
			        .warn("Sending response to failed login request {} after delay of {} milliseconds",
			                this.rejectedLogin.getMessage(),
			                IncomingLoginRequestsChannelHandler.this.failedLoginResponseDelayMillis);
			this.ctx.sendDownstream(SendLoginRequestNackEvent
			        .reject(this.rejectedLogin));
		}
	}
}
