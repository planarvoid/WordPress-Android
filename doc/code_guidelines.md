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

## Overrides

If you override a method, make sure to add an @Override tag. If the overridden
method just calls through to super, remove it.

Bad:

    @Override
    protected void onServiceUnbound() {
        super.onServiceUnbound();
    }

[Code Style Guidelines for Contributors]: http://source.android.com/source/code-style.html
