#!/bin/sh


dirname () {
	case "$1" in
	*/*)
		echo ${1%/*}
		;;
	*\\*)
		echo ${1%\\*}
		;;
	*)
		echo .
		;;
	esac
}

CWD="$(dirname "$0")"

case "$(uname -s)" in
Darwin)
	java_submodule=macosx-java3d
	case "$(uname -r)" in
	8.*) platform=tiger;;
	*) platform=macosx;;
	esac; exe=;;
Linux)
	case "$(uname -m)" in
		x86_64) platform=linux64; java_submodule=linux-amd64;;
		*) platform=linux; java_submodule=$platform;;
	esac; exe=;;
MINGW*|CYGWIN*)
	case "$PROCESSOR_ARCHITEW6432" in
	'') platform=win32; java_submodule=$platform;;
	*) platform=win64; java_submodule=$platform;;
	esac
	exe=.exe;;
esac

test -z "$JAVA_HOME" &&
JAVA_HOME="$("$CWD"/precompiled/fiji-"$platform" --print-java-home)"

# need to clone java submodule
test -f "$JAVA_HOME/lib/tools.jar" || test -f "$JAVA_HOME/../lib/tools.jar" ||
test -f "$JAVA_HOME"/Home/lib/ext/vecmath.jar || {
	echo "No JDK found; cloning it"
	JAVA_SUBMODULE=java/$java_submodule
	git submodule init "$JAVA_SUBMODULE" && (
		URL="$(git config submodule."$JAVA_SUBMODULE".url)" &&
		mkdir -p "$JAVA_SUBMODULE" &&
		cd "$JAVA_SUBMODULE" &&
		git init &&
		git remote add -t master origin "$URL" &&
		git fetch --depth 1 &&
		git reset --hard origin/master
	) || {
		echo "Could not clone JDK" >&2
		exit 1
	}
}

handle_variables () {
	case "$1" in
	--strip) strip_variables=t; shift;;
	*) strip_variables=;;
	esac
	while test $# -ge 1
	do
		case "$1" in
		*=*) test ! -z "$strip_variables" || echo "$1";;
		*) test -z "$strip_variables" || echo "$1";;
		esac
		shift
	done
}

targets=$(handle_variables --strip "$@")
variables=$(handle_variables "$@")

jar=jars/fake.jar
pre_jar=precompiled/${jar##*/}
source_dir=src-plugins/fake/fiji/build/
source=$source_dir/Fake.java

# make sure fake.jar is up-to-date
test "a$targets" != a$jar &&
test ! -f "$CWD"/$jar -o "$CWD"/$source -nt "$CWD"/$jar && {
	sh "$0" $variables $jar || exit
}

# make sure the Fiji launcher is up-to-date
test "a$targets" != a$jar -a "a$targets" != afiji &&
test ! -f "$CWD"/fiji -o "$CWD"/fiji.cxx -nt "$CWD"/fiji$exe && {
	sh "$0" $variables fiji || exit
}

# still needed for Windows, which cannot overwrite files that are in use
test -f "$CWD"/fiji$exe -a -f "$CWD"/$jar &&
test "a$targets" != a$jar -a "a$targets" != afiji &&
exec "$CWD"/fiji$exe --build "$@"

# fall back to precompiled
test -f "$CWD"/precompiled/fiji-$platform$exe \
	-a -f "$CWD"/precompiled/${jar##*/} &&
exec "$CWD"/precompiled/fiji-$platform$exe --build -- "$@"

export SYSTEM_JAVA=java
export SYSTEM_JAVAC=javac

# If JAVA_HOME leads to an executable java or javac then use them:
if [ x != x$JAVA_HOME ]
then
    if [ -e $JAVA_HOME/bin/java ]
    then
        export SYSTEM_JAVA=$JAVA_HOME/bin/java
    fi
    if [ -e $JAVA_HOME/bin/javac ]
    then
        export SYSTEM_JAVAC=$JAVA_HOME/bin/javac
    fi
fi

# fall back to calling Fake with system Java
test -f "$CWD"/$jar &&
$SYSTEM_JAVA -classpath "$CWD"/$jar fiji.build.Fake "$@"

# fall back to calling precompiled Fake with system Java
test -f "$CWD"/$pre_jar &&
$SYSTEM_JAVA -classpath "$CWD"/$pre_jar fiji.build.Fake "$@"

# fall back to compiling and running with system Java
$SYSTEM_JAVAC -source 1.3 -target 1.3 "$CWD"/$source &&
$SYSTEM_JAVA -classpath "$CWD"/$source_dir fiji.build.Fake "$@"
