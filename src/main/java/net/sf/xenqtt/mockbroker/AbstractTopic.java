package net.sf.xenqtt.mockbroker;

import net.sf.xenqtt.MqttInvalidTopicNameException;
import net.sf.xenqtt.XenqttUtil;

/**
 * Base class for wildcard and non-wildcard topics
 */
abstract class AbstractTopic {

	final String topicName;
	private final String[] topicLevels;
	private final boolean endsWithPound;
	private final boolean isWildcard;

	AbstractTopic(String topicName, boolean isWildcard) {
		this.topicName = topicName;
		this.isWildcard = isWildcard;
		this.topicLevels = XenqttUtil.quickSplit(topicName, '/');
		this.endsWithPound = topicName.endsWith("#");
	}

	/**
	 * @return True if this is a {@link WildcardTopic}. False if it is a {@link StandardTopic}.
	 */
	public boolean isWildcardTopic() {
		return isWildcard;
	}

	/**
	 * @return True if this is a {@link StandardTopic}. False if it is a {@link WildcardTopic}.
	 */
	public boolean isStandardTopic() {
		return !isWildcard;
	}

	/**
	 * @param topicName
	 *            The topic name to check
	 * @param allowWildcards
	 *            True if wildcards are allowed. False to throw an {@link MqttInvalidTopicNameException exception} if they are not allowed and one is found
	 * 
	 * @return True if the specified topic contains wildcards, false if it does not
	 * 
	 * @throws MqttInvalidTopicNameException
	 *             If the specified topic name is invalid
	 */
	final static boolean checkWildcardAndVerifyTopic(String topicName, boolean allowWildcards) throws MqttInvalidTopicNameException {

		int lastIndex = topicName.length() - 1;

		if (topicName.contains("//")) {
			throw new MqttInvalidTopicNameException("Invalid topic. A topic level may not be empty ('//'): " + topicName);
		}
		if (topicName.charAt(lastIndex) == '/') {
			throw new MqttInvalidTopicNameException("Invalid topic. A topic may not contain a trailing slash ('/'): " + topicName);
		}

		int poundIndex = topicName.indexOf('#');
		if (poundIndex >= 0) {
			if (poundIndex == lastIndex) {
				return true;
			}
			throw new MqttInvalidTopicNameException("Invalid topic. The '#' wildcard can only be used as the last character in a topic name: " + topicName);
		}

		boolean wildcard = false;
		for (int plusIndex = topicName.indexOf('+'); plusIndex >= 0; plusIndex = topicName.indexOf('+', plusIndex + 1)) {
			wildcard = true;
			if (plusIndex > 0 && topicName.charAt(plusIndex - 1) != '/') {
				throw new MqttInvalidTopicNameException("Invalid topic. The '+' wildcard can only be used as the entire topic level in a topic name: "
						+ topicName);
			}
			if (plusIndex < lastIndex && topicName.charAt(plusIndex + 1) != '/') {
				throw new MqttInvalidTopicNameException("Invalid topic. The '+' wildcard can only be used as the entire topic level in a topic name: "
						+ topicName);
			}
		}

		if (wildcard && !allowWildcards) {
			throw new MqttInvalidTopicNameException("Invalid topic. Wildcards are not allowed when publishing to a topic: " + topicName);
		}

		return wildcard;
	}

	/**
	 * @return True if this topic's name matches the specified topic name including wildcard resolution.
	 */
	final boolean nameMatches(AbstractTopic that) {

		// if they are not the same depth and neither ends in # they can't match
		if (that.topicLevels.length != this.topicLevels.length && that.endsWithPound && this.endsWithPound) {
			return false;
		}

		int size = topicLevels.length < this.topicLevels.length ? topicLevels.length : this.topicLevels.length;
		for (int i = 0; i < size; i++) {

			String s1 = topicLevels[i];
			String s2 = this.topicLevels[i];

			// if either has a # the rest doesn't matter - presumably the topic was already validated so # can only be the last level
			if ("#".equals(s1) || "#".equals(s2)) {
				return true;
			}

			// if neither has a + and the this level is not equal then the topics don't match
			if (!"+".equals(s1) && !"+".equals(s2) && !s1.equals(s2)) {
				return false;
			}
		}

		// if everything else matched then they are equal only if they have the same length
		return topicLevels.length == this.topicLevels.length;
	}
}
