# Android code guidelines

Please use Google's [Code Style Guidelines for Contributors][].

*Note:* When using Intellij IDEA, copy `doc/codestyles/android.xml` to
`$HOME/Library/Preferences/IntelliJIdea10CE/codestyles/` and select the "android"
code style in the project settings.

## File format

  * UTF-8 CRLF, no DOS file endings.
  * Spaces instead of tabs, 4 spaces indentation.
  * No trailing WS
  * Friendly to people without IDEs: column width max ~ 115.

## Import statements

No wildcards, using the following order:

 * import static
 * blank
 * import all other imports
 * blank
 * import android.*
 * blank
 * import java.*

## If statements

Always use braces.

Bad:

    if (waveformResult == BindResult.LOADING)
        mOverlay.setVisibility(View.INVISIBLE);
    else
        mOverlay.setImageDrawable(mPlayer.getResources()
              .getDrawable(R.drawable.player_wave_bg));

Good:

    if (waveformResult == BindResult.LOADING) {
        mOverlay.setVisibility(View.INVISIBLE);
    } else {
        mOverlay.setImageDrawable(mPlayer.getResources()
              .getDrawable(R.drawable.player_wave_bg));
    }

Leave braces out if it's a very simple statement and still readable without
braces.

Example:

    if (someThingIsTrue) doSomethingElse();

## Overrides

If you override a method, make sure to add an @Override tag. If the overridden
method just calls through to super, remove it.

Bad:

    @Override
    protected void onServiceUnbound() {
        super.onServiceUnbound();
    }

## Code provenance

Please add links for non-trivial snippets of code taken from other sources (Stackoverflow etc.)

## Vertical whitespace

Try to avoid adding unnecessary vertical whitespace:

Bad:

     private String mDurationFormatLong;

     private String mDurationFormatShort;

     private String mCurrentDurationString;

Good:

     private String mDurationFormatLong;
     private String mDurationFormatShort;
     private String mCurrentDurationString;

     // or alternatively, when grouping makes sense:
     private String mCurrentDurationString, mDurationFormatShort, mCurrentDurationString;

## Dead code

If code is no longer used (ideally set up your IDE to check for this
automatically) just delete it. No commenting out (it can always be brought back
using version control).

## Explicit package access

If you use package scope for fields/methods, it's a good idea to make it more
explicit by adding a comment `/* package */` just before the declaration. This
indicates that package scope is requested and not just accidental.

## Final keyword

The `final` keyword is useful in Java, and it's not for performance reason.
Read and understand [The Final Word On The final keyword][]. It's mostly useful
when declaring local variables, fields and parameters. There are cases where
final is required anyway (when accessing variables from inner classes) and it
is useful to make sure that variables are treated as constants. Use it whenever
applicable.

Example (local variable):

    // here we perform a lookup of a view - we'll never going to reassign `where`
    final EditText where = (EditText) findViewById(R.id.where);

Example (instance fields):

    // declaring it as final ensures that `mName` is initialized
    private final String mName;
    public MyContructor(String param) {
      mName = param;
    }

## Avoid too many returns

Multiple return points in a method are confusing. Returning at the top of a
method is normally ok (to ensure a parameter contract etc)

Bad:

    if (something) {
      foo();
      return;
    }

    if (someOtherCondition) {
      doSomething();
      return
    }

    doAnotherThing();

Good:

    if (something) {
      foo();
    } else if (someOtherCondition) {
      doSomething();
    } else {
      doAnotherThing();
    }
    // no return statement needed!


## Methods returning booleans

Should start with is:

Bad:

    public boolean enabled() {}

Good:

    public boolean isFooEnabled() { }

## Inner classes

Only use when appropriate (small classes) - avoid non-static innerclasses if possible.

## public, then private

Public methods should be at the top of the source code, followed by package and
private methods.

```java
public void doSomething() {
  internalMethod();
}

public void anotherPublicMethod() {
}

private void internalMethod() {
}
```


[Code Style Guidelines for Contributors]: http://source.android.com/source/code-style.html
[The Final Word on the final keyword]: http://renaud.waldura.com/doc/java/final-keyword.shtml


