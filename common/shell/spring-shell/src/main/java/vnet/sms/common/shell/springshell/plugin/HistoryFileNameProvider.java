/*
 * Copyright 2011-2012 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vnet.sms.common.shell.springshell.plugin;

/**
 * History file name provider. Plugin should implements this interface to
 * customize history file. <code>getOrder</code> indicate the priority, higher
 * values can be interpreted as lower priority
 * 
 * @author Jarred Li
 * @since 1.0
 * 
 */
public interface HistoryFileNameProvider extends PluginProvider {

	/**
	 * get history file name
	 * 
	 * @return history file name
	 */
	String getHistoryFileName();

}
