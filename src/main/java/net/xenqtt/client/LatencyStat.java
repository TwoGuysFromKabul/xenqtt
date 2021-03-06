/**
    Copyright 2013 James McClure

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package net.xenqtt.client;

/**
 * Specifies a type that provides latency-based statistics for disparate Xenqtt operations.
 */
public interface LatencyStat {

	/**
	 * @return The count of events that have been received by this stat
	 */
	long getCount();

	/**
	 * @return The minimum latency value received thus far
	 */
	long getMin();

	/**
	 * @return The maximum latency value received thus far
	 */
	long getMax();

	/**
	 * @return The average latency
	 */
	double getAverage();

}
