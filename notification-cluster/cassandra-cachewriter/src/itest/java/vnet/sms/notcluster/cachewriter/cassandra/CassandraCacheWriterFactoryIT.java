/**
 * 
 */
package vnet.sms.notcluster.cachewriter.cassandra;

import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import net.sf.ehcache.writer.CacheWriter;

import org.junit.Test;

/**
 * @author obergner
 * 
 */
public class CassandraCacheWriterFactoryIT {

	private final CassandraCacheWriterFactory	objectUnderTest	= new CassandraCacheWriterFactory();

	/**
	 * Test method for
	 * {@link vnet.sms.notcluster.cachewriter.cassandra.CassandraCacheWriterFactory#createCacheWriter(net.sf.ehcache.Ehcache, java.util.Properties)}
	 * .
	 */
	@Test
	public final void assertThatFactoryCreatesDefaultCassandraCacheWriterFromEmptyProperties() {
		final CacheWriter product = this.objectUnderTest.createCacheWriter(
		        null, new Properties());
		assertNotNull(
		        "createCacheWriter(null, new Properties()) returned null",
		        product);
	}
}
