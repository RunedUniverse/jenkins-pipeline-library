package net.runeduniverse.lib.tools.jenkins;

interface VersionSystem extends Serializable {

	public String getId();

	public setWorkflow(Object workflow);
	
	public boolean checkVersionTag(String projectId, String projectVersion);
}