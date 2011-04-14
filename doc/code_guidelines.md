# Android code guidelines

Please use Google's [Code Style Guidelines for Contributors][].

## File format

  * UTF-8 CRLF, no DOS file endings.
  * Spaces instead of tabs, 4 spaces indentation.
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


[Code Style Guidelines for Contributors]: http://source.android.com/source/code-style.html
