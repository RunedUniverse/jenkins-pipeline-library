package net.runeduniverse.lib.tools.jenkins;

@Grab('net.runeduniverse.lib.utils:utils-common:1.1.0')
import net.runeduniverse.lib.utils.common.StringUtils;

class Maven implements BuildTool {

	protected final Object workflow;
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
		return new MavenProject(this, conf);
	}

	public String exec(String path, String cmd, String args = null) {
		if(StringUtils.isBlank(args)) {
			args = "";
		}
		String result;
		this.workflow.dir(path: path) {
			result = this.workflow.sh(
					returnStdout: true,
					script: "${this.workflow.tool this.tool}/bin/mvn ${args} ${cmd}"
					);
		};
		return result;
	}

	public String execDev(String path, String cmd, String args = null) {
		String globalSettingsFile = this.workflow.getProperty("GLOBAL_MAVEN_SETTINGS");
		if(StringUtils.isBlank(globalSettingsFile)) {
			globalSettingsFile = "";
		} else {
			globalSettingsFile = "-gs " + globalSettingsFile;
		}

		String settingsFile = this.workflow.getProperty("MAVEN_SETTINGS");
		if(StringUtils.isBlank(settingsFile)) {
			settingsFile = "";
		} else {
			settingsFile = "-s " + settingsFile;
		}

		String toolchains = this.workflow.getProperty("MAVEN_TOOLCHAINS");
		if(StringUtils.isBlank(toolchains)) {
			toolchains = "";
		} else {
			toolchains = "--global-toolchains " + toolchains;
		}
		if(StringUtils.isBlank(args)) {
			args = "";
		}

		String result;
		this.workflow.dir(path: path) {
			result = this.workflow.sh(
					returnStdout: true,
					script: "${this.workflow.tool this.tool}/bin/mvn ${globalSettingsFile} ${settingsFile} ${toolchains} ${cmd} ${args}"
					);
		};
		return result;
	}

	public void purgeCache(String path) {
		execDev(path, "-P ${this.workflow.getProperty("REPOS")} dependency:purge-local-repository -DactTransitively=false -DreResolve=false --non-recursive")
	}

	public void resolveDependencies(String path) {
		execDev(path, "-P ${this.workflow.getProperty("REPOS")} dependency:resolve --non-recursive")
	}

	public String eval(String expression, String path, String modules) {
		String result;
		return exec(path, "org.apache.maven.plugins:maven-help-plugin:evaluate -Dexpression=${expression} -q -DforceStdout -pl=${modules}");
	}
}
