@Grab('net.runeduniverse.lib.utils:utils-common:1.1.0')
import net.runeduniverse.lib.utils.common.StringUtils

def call(Map cnf = [:]) {
	// acquire scripts
	loadScript(name: 'mvn-dev')
	// validate resources
	def path = ""
	if(StringUtils.isBlank("${cnf.path}")){
		path = '.'
	} else {
		path = "${cnf.path}"
	}
	def modules = ""
	if(!StringUtils.isBlank("${cnf.modules}")){
		modules = "-pl=${cnf.modules}"
	}
	// execute
	dir(path: path) {
		sh ".scripts/mvn-dev -P ${env.REPOS},${cnf.profiles} ${cnf.goals} ${modules}"
	}
}

