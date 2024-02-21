package net.runeduniverse.lib.tools.jenkins;

interface Project extends Serializable {

	public String getId();

	public String getName();

	public String getPath();

	public String getVersion();

	public boolean isParent();

	public boolean hasChanged();

	public boolean isActive();

	public boolean isBOM();

	public void setChanged(boolean changed);

	public void setActive(boolean active);

	public void attachTo(PipelineBuilder builder);

	//public List<Project> collectProjects(Map config);

	public void purgeCache();

	public void resolveResources();

	public void info();

	public void info(boolean interate);
}