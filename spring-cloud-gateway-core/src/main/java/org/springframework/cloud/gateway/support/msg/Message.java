package org.springframework.cloud.gateway.support.msg;

public class Message {

	private static volatile Message instance;

	public static Message getInstance() {
		if (null == instance) {
			synchronized (Message.class) {
				if (null == instance) {
					instance = new Message();
				}
			}
		}

		return instance;
	}

	public String getProjectNoResourceGroup(String project) {
		return String.format("No resource group has been allocated to the project '%s' yet. Please contact your administrator to allocate a resource group for it.", project);
	}

	public String getProjectNoInstance(String project) {
		return String.format("No available instance has been allocated to the project '%s' yet. Please contact your administrator to check the instance and retry.", project);
	}

	public String getNoInstance() {
		return "No available instance. Please contact your administrator to check the instance and retry.";
	}
}
