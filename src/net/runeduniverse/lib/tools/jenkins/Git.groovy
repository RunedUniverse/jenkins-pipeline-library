package net.runeduniverse.lib.tools.jenkins;

class Git implements VersionSystem {

	def workflow;
	private final String id;

	public Git(String id = "git") {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public setWorkflow(Object workflow) {
		this.workflow = workflow;
	}

	public boolean versionTagExists(String projectId, String projectVersion) {
		boolean found = this.workflow.sh(
				returnStdout: true,
				script: "git describe --tags --abbrev=0 remotes/origin/master --match ${projectId}/v${projectVersion} &> /dev/null && printf 1"
				);
		if(found.equals("1")) {
			return true;
		}
		return false;
	}
}
