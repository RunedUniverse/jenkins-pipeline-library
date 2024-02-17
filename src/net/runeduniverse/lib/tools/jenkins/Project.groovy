package net.runeduniverse.lib.tools.jenkins;

interface Project extends Serializable {

	public String getId();

	public String getName();

	public String getPath();

	public String getVersion();

	public boolean hasChanged();

	public void setChanged(boolean changed);
	
	public void attachTo(PipelineBuilder builder);

	public void info();

	public void info(boolean interate);
}