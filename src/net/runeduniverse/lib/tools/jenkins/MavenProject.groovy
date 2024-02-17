package net.runeduniverse.lib.tools.jenkins;

@Grab(group='net.runeduniverse.lib.utils', module='utils-logging', version='1.0.2')
import net.runeduniverse.lib.utils.logging.logs.CompoundTree;

class MavenProject implements Project {

	def workflow;

	private String name = "";
	private String path = ".";
	private String modulePath = ".";
	private MavenProject parent = null;
	private List<MavenProject> modules = new LinkedList();

	MavenProject(workflow){
		this.workflow = workflow;
	}

	public MavenProject setName(name) {
		this.name = name;
		return this;
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

	public MavenProject addModule(Map conf) {
		MavenProject project = new MavenProject(this.workflow)
				.setName(conf.name)
				.setPath(conf.path)
				.setModulePath(conf.modulePath);
		this.addModule(project);
		return project;
	}

	public String getVersion(modulePath = null) {
		String modPath = modulePath == null ? "." : modulePath;

		if(this.parent == null) {
			String version;
			this.workflow.dir(path: this.path) {
				version = this.workflow.sh(
						returnStdout: true,
						script: "${this.workflow.tool 'maven-latest'}/bin/mvn org.apache.maven.plugins:maven-help-plugin:evaluate -Dexpression=project.version -q -DforceStdout -pl=${modPath}"
						);
			};
			return version;
		}
		modPath = this.modulePath == null ? this.path : this.modulePath;
		if(modulePath != null) {
			modPath = modPath + '/' + modulePath;
		}
		return this.parent.getVersion(modPath);
	}

	protected CompoundTree _info() {
		final CompoundTree tree = new CompoundTree("Module", this.name)
				.append("path", this.path)
				.append("module_path", this.modulePath)
				.append("version", this.getVersion());
		this.modules.each {
			tree.append(it._info());
		}
		return tree;
	}

	public void info() {
		this.workflow.sh("printf 'Maven Project:\n" + this._info() + "\n'");
	}
}