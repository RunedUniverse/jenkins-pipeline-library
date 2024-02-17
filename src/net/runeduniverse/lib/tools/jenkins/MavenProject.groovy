package net.runeduniverse.lib.tools.jenkins;

class MavenProject implements Project {

	def workflow;

	private String path = ".";
	private String modulePath = ".";
	private MavenProject parent = null;
	private List<MavenProject> modules = new LinkedList();

	MavenProject(workflow){
		this.workflow = workflow;
	}

	public MavenProject setPath(path) {
		this.path = path;
		return this;
	}
	public MavenProject setModulePath(modulePath) {
		this.modulePath = modulePath;
		return this;
	}

	protected void setParent(MavenProject parent) {
		this.parent = parent;
	}

	public MavenProject addModule(MavenProject project) {
		project.setParent(this);
		this.modules.add(project);
		return this;
	}

	public String getVersion(modulePath = null) {
		def modPath = modulePath == null ? "." : modulePath;

		if(this.parent==null) {
			this.workflow.dir(
					path: this.path, {
						return this.workflow.sh(
								returnStdout: true,
								script: "${this.workflow.tool 'maven-latest'}/bin/mvn org.apache.maven.plugins:maven-help-plugin:evaluate -Dexpression=project.version -q -DforceStdout -pl=${this.path}"
								);
					});
		}

		modPath = modulePath == null ? this.modulePath : this.modulePath + '/' + modulePath;
		return this.parent.getVersion(modPath);
	}

	def info() {
		this.workflow.sh "echo Method 1: ${this.getVersion()}"
	}
}