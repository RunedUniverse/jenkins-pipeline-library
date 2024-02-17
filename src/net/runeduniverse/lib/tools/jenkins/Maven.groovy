package net.runeduniverse.lib.tools.jenkins;

class Maven implements BuildTool {

	private final Object workflow;
	private String tool = "maven-latest";

	Maven(workflow){
		this.workflow = workflow;
	}

	public String getId() {
		return "maven";
	}

	public String getTool() {
		return tool;
	}

	public void setTool(String tool) {
		this.tool = tool;
	}

	public MavenProject createProject(Map conf) {
		return new MavenProject(this.workflow, this, conf);
	}

	public String exec(String path, String cmd) {
		String result;
		this.workflow.dir(path: path) {
			result = this.workflow.sh(
					returnStdout: true,
					script: "${this.workflow.tool this.tool}/bin/mvn ${cmd}"
					);
		};
		return result;
	}

	public String execDev(String path, String cmd) {
		String globalSettingsFile = this.workflow.getProperty("GLOBAL_MAVEN_SETTINGS");
		if(globalSettingsFile == null) {
			globalSettingsFile = "";
		}else {
			globalSettingsFile = "-gs " + globalSettingsFile;
		}

		String settingsFile = this.workflow.getProperty("MAVEN_SETTINGS");
		if(settingsFile == null) {
			settingsFile = "";
		}else {
			settingsFile = "-s " + settingsFile;
		}

		String toolchains = this.workflow.getProperty("MAVEN_TOOLCHAINS");
		if(toolchains == null) {
			toolchains = "";
		}else {
			toolchains = "--global-toolchains " + toolchains;
		}

		String result;
		this.workflow.dir(path: path) {
			result = this.workflow.sh(
					returnStdout: true,
					script: "${this.workflow.tool this.tool}/bin/mvn ${globalSettingsFile} ${settingsFile} ${toolchains} ${cmd}"
					);
		};
		return result;
	}

	public void purgeCache() {
		execDev(".", "-P ${this.workflow.getProperty("REPOS")} dependency:purge-local-repository -DactTransitively=false -DreResolve=false --non-recursive")
	}

	public void resolveDependencies(String path) {
		execDev(path, "-P ${this.workflow.getProperty("REPOS")} dependency:resolve --non-recursive")
	}

	public String eval(String expression, String path, String modules) {
		String result;
		return exec(path, "org.apache.maven.plugins:maven-help-plugin:evaluate -Dexpression=${expression} -q -DforceStdout -pl=${modules}");
	}
}
