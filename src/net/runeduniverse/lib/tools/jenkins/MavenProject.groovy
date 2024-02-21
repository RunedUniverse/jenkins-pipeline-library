package net.runeduniverse.lib.tools.jenkins;

@Grab('net.runeduniverse.lib.utils:utils-common:1.1.0')
import net.runeduniverse.lib.utils.common.StringUtils;

class MavenProject implements Project {

	private final Object workflow;
	private final Maven mvn;

	private String id = "";
	private String name = "";
	private String path = ".";
	private String modulePath = null;
	private String packagingProcedure = null;
	private String version = null;
	private Boolean changed = null;
	private boolean active = true;
	private boolean bom = false;
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

	public String getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public String getPath() {
		return this.path;
	}

	@NonCPS
	public String getModulePath() {
		return this.modulePath == null ? this.path : this.modulePath;
	}

	public boolean isParent() {
		return this.parent == null;
	}

	public boolean hasChanged() {
		return this.changed == null ? true : this.changed;
	}

	public boolean isActive() {
		return this.active;
	}

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

	@NonCPS
	public String eval(String expression, String modulePath = null) {
		if(this.parent == null) {
			return this.mvn.eval(expression, this.path, modulePath == null ? "." : modulePath);
		}
		String modPath = getModulePath();
		if(modulePath != null && !".".equals(modulePath)) {
			modPath = modPath + '/' + modulePath;
		}
		return this.parent.eval(expression, modPath);
	}

	@NonCPS
	public String getVersion() {
		if(this.version == null) {
			this.version = getVersion(".");
		}
		return this.version;
	}

	@NonCPS
	public String getVersion(String modulePath) {
		return eval("project.version", modulePath);
	}

	@NonCPS
	public String getPackagingProcedure() {
		if(this.packagingProcedure == null) {
			this.packagingProcedure = getPackagingProcedure(".");
		}
		return this.packagingProcedure;
	}

	@NonCPS
	public String getPackagingProcedure(String modulePath) {
		return eval("project.packaging", modulePath);
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

	// mark with @NonCPS so Jenkins doesn't try transform it
	// @NonCPS can't execute Pipeline steps!!!
	@NonCPS
	private List<String> toStringList(Object obj){
		List<String> result = new LinkedList();
		if(obj instanceof List) {
			result.addAll(obj.toUnique().grep {
				it instanceof String && !StringUtils.isBlank(it)
			}.collect {
				it.trim()
			});
		} else {
			if(obj instanceof String && !StringUtils.isBlank(obj)) {
				result.add(obj.trim());
			}
		}
		return result;
	}

	public String execDev(Map cnf = [:]) {
		List<String> modules = toStringList(cnf.modules);
		if(this.parent == null || Boolean.TRUE.equals(cnf.skipParent)) {
			List<String> profiles = new LinkedList();
			if(!Boolean.TRUE.equals(cnf.skipRepos)) {
				profiles.add(this.workflow.getProperty("REPOS"));
			}
			profiles.addAll(toStringList(cnf.profiles));
			String profilesArg = profiles.isEmpty() ? "" : "-P " + profiles.join(",");
			String goals = toStringList(cnf.goals).join(",");
			String modulesArg = modules.isEmpty() ? "" : "-pl=" + modules.join(",");
			this.workflow.echo("path:       " + this.path);
			return this.mvn.execDev(
					this.path,
					"${profilesArg} ${goals} ${modulesArg}",
					toStringList(cnf.args).join(" ")
					);
		}
		String modPath = this.modulePath == null ? this.path : this.modulePath;
		if(modules.isEmpty()) {
			// compile this and all children
			modules.add(modPath + "/*");
		} else {
			modules = modules.collect {
				it.equals(".") ? modPath : modPath + '/' + it
			}
		}
		cnf.modules = modules;
		return this.parent.execDev(cnf);
	}

	@NonCPS
	public String toRecord() {
		String tree = "";
		tree = tree +  "id:                ${this.id}\n";
		tree = tree +  "name:              ${this.name}\n";
		tree = tree +  "path:              ${this.path}\n";
		tree = tree +  "modulePath:        ${this.getModulePath()}\n";
		tree = tree +  "version:           ${this.getVersion()}\n";
		tree = tree +  "packaging (proc.): ${this.getPackagingProcedure()}\n";
		tree = tree +  "version changed:   ${this.changed == null ? "????" : this.changed.toString()}\n";
		return tree;
	}

	public void info(boolean interate = true) {
		this.workflow.echo(this.toRecord());

		if(interate) {
			for (m in this.modules) {
				this.workflow.echo("-------------------------");
				m.info();
			}
		}
	}
}