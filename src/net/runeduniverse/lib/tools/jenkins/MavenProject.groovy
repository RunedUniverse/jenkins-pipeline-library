package net.runeduniverse.lib.tools.jenkins;

class MavenProject implements Project {

	protected final Object workflow;
	protected final Maven mvn;

	private String id = "";
	private String name = "";
	private String path = ".";
	private String modulePath = null;
	private String packagingProcedure = null;
	private String version = null;
	private Boolean changed = null;
	private boolean active = true;
	private boolean bom = false;
	protected MavenProject parent = null;
	protected List<MavenProject> modules = new LinkedList();

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
		if(conf.active != null) {
			this.active = conf.active == true;
		}
		if(conf.bom != null) {
			this.bom = conf.bom == true;
		}
	}

	////////////////////////////////////////////////////////////
	// GETTER
	////////////////////////////////////////////////////////////

	@NonCPS
	public String getId() {
		return this.id;
	}

	@NonCPS
	public String getName() {
		return this.name;
	}

	@NonCPS
	public String getPath() {
		return this.path;
	}

	@NonCPS
	public String getModulePath() {
		return this.modulePath == null ? this.path : this.modulePath;
	}

	@NonCPS
	public MavenProject getParent() {
		return parent;
	}

	@NonCPS
	public boolean isParent() {
		return parent == null;
	}

	@NonCPS
	public boolean hasChanged() {
		return this.changed == null ? true : this.changed;
	}

	@NonCPS
	public boolean isActive() {
		return this.active;
	}

	@NonCPS
	public boolean isBOM() {
		return this.bom;
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

	public void setActive(boolean active) {
		this.active = active;
	}

	////////////////////////////////////////////////////////////

	public MavenProject addModule(MavenProject project) {
		project.parent = this;
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
		for (m in this.modules) {
			m.attachTo(builder);
		}
	}

	@NonCPS
	public String getVersion() {
		if(version == null) {
			version = PUtils.mvnEval(this, "project.version", null);
		}
		return version;
	}

	@NonCPS
	public String getPackagingProcedure() {
		if(packagingProcedure == null) {
			packagingProcedure = PUtils.mvnEval(this, "project.packaging", null);
		}
		return packagingProcedure;
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

	public List<Project> collectProjects(Map config) {
		return PUtils.collectMvnModules(this, config);
	}

	public void info(boolean interate = true) {
		this.workflow.echo("id:                ${this.id}");
		this.workflow.echo("name:              ${this.name}");
		this.workflow.echo("path:              ${this.path}");
		this.workflow.echo("modulePath:        ${this.getModulePath()}");
		this.workflow.echo("version:           ${this.getVersion()}");
		this.workflow.echo("packaging (proc.): ${this.getPackagingProcedure()}");
		this.workflow.echo("version changed:   ${this.changed == null ? "????" : this.changed.toString()}");

		if(interate) {
			for (m in this.modules) {
				this.workflow.echo("-------------------------");
				m.info();
			}
		}
	}
}