package net.runeduniverse.lib.tools.jenkins;

@Grab('net.runeduniverse.lib.utils:utils-common:1.1.0')
import net.runeduniverse.lib.utils.common.StringUtils;

class MavenProject implements Project {

	private final Object workflow;
	private final Maven mvn;

	private String id = "";
	private String name = "";
	private String path = ".";
	private String modulePath = ".";
	private Boolean changed = null;
	private MavenProject parent = null;
	private List<MavenProject> modules = new LinkedList();

	MavenProject(Maven mvn){
		this.mvn = mvn;
		this.workflow = this.mvn.workflow;
	}
	MavenProject(Maven mvn, Map conf){
		this.mvn = mvn;
		this.workflow = this.mvn.workflow;
		this.id = conf.id;
		this.name = conf.name;
		this.path = conf.path;
		this.modulePath = conf.modulePath;
	}

	////////////////////////////////////////////////////////////
	// GETTER
	////////////////////////////////////////////////////////////

	public String getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public String getPath() {
		return this.path;
	}

	public boolean hasChanged() {
		return this.changed == null ? true : this.changed;
	}

	////////////////////////////////////////////////////////////
	// SETTER
	////////////////////////////////////////////////////////////

	public MavenProject setId(String id) {
		this.id = id;
		return this;
	}

	public MavenProject setName(String name) {
		this.name = name;
		return this;
	}
	public MavenProject setPath(String path) {
		this.path = path;
		return this;
	}

	public void setChanged(boolean changed) {
		this.changed = changed;
	}

	public MavenProject setModulePath(String modulePath) {
		this.modulePath = modulePath;
		return this;
	}

	protected void setParent(MavenProject parent) {
		this.parent = parent;
	}

	////////////////////////////////////////////////////////////

	public MavenProject addModule(MavenProject project) {
		project.setParent(this);
		this.modules.add(project);
		return this;
	}

	public MavenProject addModule(Map conf) {
		MavenProject project = new MavenProject(this.mvn, conf);
		this.addModule(project);
		return project;
	}

	public void attachTo(PipelineBuilder builder) {
		builder.attachProject(this);
		this.modules.each {
			it.attachTo(builder);
		}
	}

	public String getVersion(modulePath = null) {
		String modPath = modulePath == null ? "." : modulePath;

		if(this.parent == null) {
			return this.mvn.eval("project.version", this.path, modPath);
		}
		modPath = this.modulePath == null ? this.path : this.modulePath;
		if(modulePath != null) {
			modPath = modPath + '/' + modulePath;
		}
		return this.parent.getVersion(modPath);
	}

	public void purgeCache() {
		if(this.parent != null)
			return;
		this.mvn.purgeCache(this.path);
	}

	public void resolveResources() {
		if(this.parent != null)
			return;
		this.mvn.resolveDependencies(this.path);
	}

	public String execDev(Map cnf = [:]) {
		String modPath = cnf.module == null ? "." : cnf.module;

		if(this.parent == null) {
			String profiles = ""
			if(!StringUtils.isBlank(cnf.profiles)) {
				profiles = cnf.profiles;
			}
			String goals = ""
			if(!StringUtils.isBlank(cnf.goals)) {
				goals = cnf.goals;
			}
			return this.mvn.execDev(this.path, "-P ${this.workflow.getProperty("REPOS")},${cnf.profiles} ${cnf.goals} -pl=${modPath}");
		}
		modPath = this.modulePath == null ? this.path : this.modulePath;
		if(modulePath != null) {
			cnf.module = modPath + '/' + cnf.module;
		}
		return this.parent.getVersion(cnf);
	}

	public void info(boolean interate = true) {
		this.workflow.echo("id:         " + this.id);
		this.workflow.echo("name:       " + this.name);
		this.workflow.echo("path:       " + this.path);
		if(this.modulePath != null)
			this.workflow.echo("modulePath: " + this.modulePath);
		this.workflow.echo("version:    " + this.path);
		this.workflow.echo("changed:    " + this.changed == null ? "????" : this.changed.toString());

		if(interate) {
			this.modules.each {
				this.workflow.echo("-------------------------");
				it.info();
			}
		}
	}
}