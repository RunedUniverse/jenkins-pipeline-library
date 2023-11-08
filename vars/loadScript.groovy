def call(Map cnf = [:]) {
  def scriptcontents = libraryResource "net/runeduniverse/scripts/linux/${cnf.name}"    
  writeFile file: ".scripts/${cnf.name}", text: scriptcontents
}

