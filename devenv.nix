{ pkgs, lib, config, inputs, ... }:

{
  env.GREET = "devenv";

  packages = [
    pkgs.git
    pkgs.maven
  ];

  languages.java = {
  enable = true;
  jdk.package = pkgs.jdk25;
  };

  enterShell = ''
    ln -sfn "$JAVA_HOME" "$PWD/.jdk"
  '';

  enterTest = ''
    echo "Running tests"
    git --version | grep --color=auto "${pkgs.git.version}"
  '';
}
