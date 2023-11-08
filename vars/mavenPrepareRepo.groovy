def call(Map cnf = [:]) {
	loadScript(name: 'mvn-dev')
	dir(path: "${cnf.path}") {
		sh ".scripts/mvn-dev -P ${env.REPOS} dependency:purge-local-repository -DactTransitively=false -DreResolve=false --non-recursive"
		sh ".scripts/mvn-dev -P ${env.REPOS} dependency:resolve --non-recursive"
	}
}

