package net.runeduniverse.lib.tools.jenkins;

class MavenProject implements Project {

	def workflow;
	private final Maven mvn;

	private String id = "";
	private String name = "";
	private String path = ".";
	private String modulePath = ".";
	private Boolean changed = null;
	private MavenProject parent = null;
	private List<MavenProject> modules = new LinkedList();

	MavenProject(Object workflow, Maven mvn){
		this.workflow = workflow;
		this.mvn = mvn;
	}
	MavenProject(Object workflow, Maven mvn, Map conf){
		this.workflow = workflow;
		this.mvn = mvn;
		this.setId(conf.id)
				.setName(conf.name)
				.setPath(conf.path)
				.setModulePath(conf.modulePath);
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
	
	public void resolveResources() {
		if(this.parent != null)
			return;
		this.mvn.resolveDependencies(this.path);
	}

	public void info(boolean interate = true) {
		this.workflow.sh("echo id:         " + this.id);
		this.workflow.sh("echo name:       " + this.name);
		this.workflow.sh("echo path:       " + this.path);
		if(this.modulePath != null)
			this.workflow.sh("echo modulePath: " + this.modulePath);
		this.workflow.sh("echo version:    " + this.path);
		this.workflow.sh("echo changed:    " + this.changed == null ? "????" : this.changed.toString());

		if(interate) {
			this.modules.each {
				this.workflow.sh("echo -------------------------");
				it.info();
			}
		}
	}
}