command -v pidcat >/dev/null 2>&1 || { echo >&2 "You need to install pidcat: brew install pidcat"; exit 1; }

GREP_PATTERN=""
for tagName in "$@"
do
	adb shell setprop log.tag.$tagName DEBUG

	if [ -n "$GREP_PATTERN" ]; then
	    GREP_PATTERN="${GREP_PATTERN}|"
	fi
    GREP_PATTERN="${GREP_PATTERN}$tagName"
done
GREP_PATTERN="(${GREP_PATTERN})"

pidcat com.soundcloud.android --always-display-tags | egrep $GREP_PATTERN
