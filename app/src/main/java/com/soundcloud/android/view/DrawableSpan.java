package com.soundcloud.android.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.ReplacementSpan;

public class DrawableSpan extends ReplacementSpan
{
	/**
	 * A constant indicating that the bottom of this span should be aligned with
	 * the bottom of the surrounding text, i.e., at the same level as the lowest
	 * descender in the text.
	 */
	public static final int ALIGN_BOTTOM = 0;

	/**
	 * A constant indicating that the bottom of this span should be aligned with
	 * the baseline of the surrounding text.
	 */
	public static final int ALIGN_BASELINE = 1;

	protected final Drawable d;
	protected final int valign;

	/**
	 * @param valign
	 *            one of {@link #ALIGN_BOTTOM} or {@link #ALIGN_BASELINE}.
	 */
	public DrawableSpan(Drawable _d, int _valign)
	{
		d = _d;
		valign = _valign;
	}

	/**
	 * Returns the vertical alignment of this span, one of {@link #ALIGN_BOTTOM}
	 * or {@link #ALIGN_BASELINE}.
	 */
	public int getValign()
	{
		return valign;
	}
	public Drawable getDrawable()
	{
		return d;
	}

	@Override
	public int getSize(Paint paint, CharSequence text, int start, int end,
			           Paint.FontMetricsInt fm)
	{
		Rect rect = d.getBounds();

		if (fm != null)
		{
			fm.ascent = -rect.bottom;
			fm.descent = 0;

			fm.top = fm.ascent;
			fm.bottom = 0;
		}

		return rect.right;
	}

	@Override
	public void draw(Canvas canvas, CharSequence text, int start, int end,
					 float x, int top, int y, int bottom, Paint paint)
	{
		canvas.save();

		int transY;
		if (ALIGN_BOTTOM == valign)
		{
			transY = bottom;
		} else { // ALIGN_BASELINE == valign
			transY = y;
		}
		transY -= d.getBounds().bottom;

		canvas.translate(x, transY);
		d.draw(canvas);
		canvas.restore();
	}

}