package com.ledgerbook.lite;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

/** Original hand-drawn category icon renderer based on category semantics. */
public final class CrayonIconView extends View {
    private String transactionType = LedgerDb.TYPE_EXPENSE;
    private String category = "其他";

    public CrayonIconView(Context context) {
        super(context);
    }

    public CrayonIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCategory(String transactionType, String category) {
        this.transactionType = transactionType;
        this.category = category == null ? "其他" : category;
        setContentDescription(this.category + "分类图标");
        invalidate();
    }

    public static Drawable drawable(String transactionType, String category) {
        return new CategoryDrawable(transactionType, category);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawIcon(canvas, new RectF(0, 0, getWidth(), getHeight()),
                CrayonIconSpec.forCategory(transactionType, category));
    }

    private static void drawIcon(Canvas canvas, RectF bounds, CrayonIconSpec spec) {
        float size = Math.min(bounds.width(), bounds.height());
        float left = bounds.centerX() - size / 2f;
        float top = bounds.centerY() - size / 2f;
        RectF box = new RectF(left, top, left + size, top + size);
        float unit = size / 100f;

        Paint fill = paint(Paint.Style.FILL, palette(spec.family), 0f);
        Paint line = paint(Paint.Style.STROKE, 0xFF4A382E, Math.max(2f, unit * 5f));
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        Paint faint = paint(Paint.Style.STROKE, 0x554A382E, Math.max(1f, unit * 2.2f));
        faint.setStrokeCap(Paint.Cap.ROUND);

        RectF badge = inset(box, unit * 5f);
        canvas.drawRoundRect(badge, unit * 25f, unit * 25f, fill);
        canvas.drawRoundRect(badge, unit * 25f, unit * 25f, line);
        RectF ghost = new RectF(badge.left + unit * 1.6f, badge.top - unit,
                badge.right - unit, badge.bottom + unit * 1.2f);
        canvas.drawRoundRect(ghost, unit * 24f, unit * 24f, faint);

        RectF art = inset(badge, unit * 21f);
        String family = spec.family;
        if (family.startsWith("income") || "finance".equals(family)) drawCoin(canvas, art, line, spec.mark);
        else if ("food".equals(family)) drawFood(canvas, art, line);
        else if ("transport".equals(family)) drawCar(canvas, art, line);
        else if ("home".equals(family)) drawHome(canvas, art, line);
        else if ("medical".equals(family)) drawMedical(canvas, art, line);
        else if ("education".equals(family)) drawBook(canvas, art, line);
        else if ("beauty".equals(family)) drawBeauty(canvas, art, line);
        else if ("family".equals(family)) drawFamily(canvas, art, line);
        else if ("shopping".equals(family)) drawBag(canvas, art, line);
        else if ("travel".equals(family)) drawSuitcase(canvas, art, line);
        else if ("entertainment".equals(family)) drawGame(canvas, art, line);
        else if ("pet".equals(family)) drawPaw(canvas, art, line);
        else if ("sport".equals(family)) drawBall(canvas, art, line);
        else if ("garden".equals(family)) drawLeaf(canvas, art, line);
        else if ("tools".equals(family)) drawHammer(canvas, art, line);
        else if ("digital".equals(family)) drawPhone(canvas, art, line);
        else if ("office".equals(family)) drawBriefcase(canvas, art, line);
        else if ("social".equals(family)) drawChat(canvas, art, line);
        else if ("utilities".equals(family)) drawUtilities(canvas, art, line);
        else if ("clothing".equals(family)) drawShirt(canvas, art, line);
        else if ("transfer".equals(family)) drawTransfer(canvas, art, line);
        else drawMark(canvas, art, line, spec.mark);
    }

    private static Paint paint(Paint.Style style, int color, float width) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(style);
        p.setColor(color);
        p.setStrokeWidth(width);
        return p;
    }

    private static int palette(String family) {
        int[] values = {0xFFFFD85A, 0xFFFFA782, 0xFF83C8F4, 0xFF9EDB82,
                0xFFC9B5F4, 0xFFFFC4D7, 0xFF9DDDD5};
        return values[Math.abs(family.hashCode()) % values.length];
    }

    private static RectF inset(RectF source, float amount) {
        return new RectF(source.left + amount, source.top + amount,
                source.right - amount, source.bottom - amount);
    }

    private static void drawFood(Canvas c, RectF r, Paint p) {
        RectF bowl = new RectF(r.left, r.centerY(), r.right, r.bottom);
        c.drawArc(bowl, 0, 180, false, p);
        c.drawLine(r.left + r.width() * .16f, r.centerY(), r.right - r.width() * .16f, r.centerY(), p);
        for (int i = 0; i < 3; i++) {
            float x = r.left + r.width() * (.28f + i * .22f);
            c.drawArc(new RectF(x - r.width() * .07f, r.top, x + r.width() * .07f,
                    r.centerY() - r.height() * .06f), 210, 120, false, p);
        }
    }

    private static void drawCar(Canvas c, RectF r, Paint p) {
        RectF body = new RectF(r.left, r.centerY() - r.height() * .08f, r.right, r.bottom - r.height() * .18f);
        c.drawRoundRect(body, r.height() * .13f, r.height() * .13f, p);
        Path roof = new Path();
        roof.moveTo(r.left + r.width() * .22f, body.top);
        roof.lineTo(r.left + r.width() * .38f, r.top + r.height() * .2f);
        roof.lineTo(r.left + r.width() * .72f, r.top + r.height() * .2f);
        roof.lineTo(r.right - r.width() * .08f, body.top);
        c.drawPath(roof, p);
        c.drawCircle(r.left + r.width() * .25f, r.bottom - r.height() * .14f, r.width() * .09f, p);
        c.drawCircle(r.right - r.width() * .25f, r.bottom - r.height() * .14f, r.width() * .09f, p);
    }

    private static void drawHome(Canvas c, RectF r, Paint p) {
        Path path = new Path();
        path.moveTo(r.left, r.centerY());
        path.lineTo(r.centerX(), r.top);
        path.lineTo(r.right, r.centerY());
        path.lineTo(r.right - r.width() * .12f, r.centerY());
        path.lineTo(r.right - r.width() * .12f, r.bottom);
        path.lineTo(r.left + r.width() * .12f, r.bottom);
        path.lineTo(r.left + r.width() * .12f, r.centerY());
        path.close();
        c.drawPath(path, p);
        c.drawRect(r.centerX() - r.width() * .1f, r.bottom - r.height() * .3f,
                r.centerX() + r.width() * .1f, r.bottom, p);
    }

    private static void drawMedical(Canvas c, RectF r, Paint p) {
        c.drawRoundRect(new RectF(r.centerX() - r.width() * .12f, r.top,
                r.centerX() + r.width() * .12f, r.bottom), r.width() * .05f, r.width() * .05f, p);
        c.drawRoundRect(new RectF(r.left, r.centerY() - r.height() * .12f,
                r.right, r.centerY() + r.height() * .12f), r.width() * .05f, r.width() * .05f, p);
    }

    private static void drawBook(Canvas c, RectF r, Paint p) {
        Path left = new Path();
        left.moveTo(r.centerX(), r.top + r.height() * .12f);
        left.quadTo(r.left + r.width() * .28f, r.top, r.left, r.top + r.height() * .16f);
        left.lineTo(r.left, r.bottom - r.height() * .05f);
        left.quadTo(r.left + r.width() * .28f, r.bottom - r.height() * .18f, r.centerX(), r.bottom);
        c.drawPath(left, p);
        Path right = new Path();
        right.moveTo(r.centerX(), r.top + r.height() * .12f);
        right.quadTo(r.right - r.width() * .28f, r.top, r.right, r.top + r.height() * .16f);
        right.lineTo(r.right, r.bottom - r.height() * .05f);
        right.quadTo(r.right - r.width() * .28f, r.bottom - r.height() * .18f, r.centerX(), r.bottom);
        c.drawPath(right, p);
        c.drawLine(r.centerX(), r.top + r.height() * .1f, r.centerX(), r.bottom, p);
    }

    private static void drawBeauty(Canvas c, RectF r, Paint p) {
        RectF tube = new RectF(r.centerX() - r.width() * .16f, r.centerY(),
                r.centerX() + r.width() * .16f, r.bottom);
        c.drawRect(tube, p);
        Path tip = new Path();
        tip.moveTo(tube.left + r.width() * .04f, r.centerY());
        tip.lineTo(tube.left + r.width() * .06f, r.top + r.height() * .12f);
        tip.quadTo(r.centerX(), r.top, tube.right - r.width() * .03f, r.top + r.height() * .2f);
        tip.lineTo(tube.right - r.width() * .04f, r.centerY());
        tip.close();
        c.drawPath(tip, p);
    }

    private static void drawFamily(Canvas c, RectF r, Paint p) {
        c.drawCircle(r.left + r.width() * .32f, r.top + r.height() * .28f, r.width() * .16f, p);
        c.drawCircle(r.right - r.width() * .32f, r.top + r.height() * .28f, r.width() * .16f, p);
        c.drawArc(new RectF(r.left, r.centerY() - r.height() * .05f,
                r.centerX() + r.width() * .12f, r.bottom), 185, 170, false, p);
        c.drawArc(new RectF(r.centerX() - r.width() * .12f, r.centerY() - r.height() * .05f,
                r.right, r.bottom), 185, 170, false, p);
    }

    private static void drawBag(Canvas c, RectF r, Paint p) {
        RectF bag = new RectF(r.left + r.width() * .08f, r.top + r.height() * .3f,
                r.right - r.width() * .08f, r.bottom);
        c.drawRoundRect(bag, r.width() * .08f, r.width() * .08f, p);
        c.drawArc(new RectF(r.centerX() - r.width() * .2f, r.top,
                r.centerX() + r.width() * .2f, r.top + r.height() * .55f), 190, 160, false, p);
    }

    private static void drawSuitcase(Canvas c, RectF r, Paint p) {
        RectF body = new RectF(r.left + r.width() * .08f, r.top + r.height() * .22f,
                r.right - r.width() * .08f, r.bottom);
        c.drawRoundRect(body, r.width() * .08f, r.width() * .08f, p);
        c.drawRoundRect(new RectF(r.centerX() - r.width() * .17f, r.top,
                r.centerX() + r.width() * .17f, r.top + r.height() * .28f),
                r.width() * .05f, r.width() * .05f, p);
        c.drawLine(r.centerX(), body.top, r.centerX(), body.bottom, p);
    }

    private static void drawGame(Canvas c, RectF r, Paint p) {
        Path pad = new Path();
        pad.moveTo(r.left + r.width() * .18f, r.centerY() - r.height() * .12f);
        pad.quadTo(r.centerX(), r.top, r.right - r.width() * .18f, r.centerY() - r.height() * .12f);
        pad.quadTo(r.right, r.bottom, r.right - r.width() * .25f, r.bottom - r.height() * .08f);
        pad.lineTo(r.centerX(), r.centerY() + r.height() * .12f);
        pad.lineTo(r.left + r.width() * .25f, r.bottom - r.height() * .08f);
        pad.quadTo(r.left, r.bottom, r.left + r.width() * .18f, r.centerY() - r.height() * .12f);
        c.drawPath(pad, p);
        c.drawLine(r.left + r.width() * .28f, r.centerY(), r.left + r.width() * .48f, r.centerY(), p);
        c.drawLine(r.left + r.width() * .38f, r.centerY() - r.height() * .1f,
                r.left + r.width() * .38f, r.centerY() + r.height() * .1f, p);
        c.drawCircle(r.right - r.width() * .3f, r.centerY() - r.height() * .06f, r.width() * .04f, p);
        c.drawCircle(r.right - r.width() * .2f, r.centerY() + r.height() * .06f, r.width() * .04f, p);
    }

    private static void drawPaw(Canvas c, RectF r, Paint p) {
        c.drawOval(new RectF(r.centerX() - r.width() * .22f, r.centerY() - r.height() * .02f,
                r.centerX() + r.width() * .22f, r.bottom), p);
        float[] xs = {.2f, .42f, .62f, .82f};
        float[] ys = {.36f, .18f, .18f, .36f};
        for (int i = 0; i < xs.length; i++) c.drawCircle(r.left + r.width() * xs[i],
                r.top + r.height() * ys[i], r.width() * .09f, p);
    }

    private static void drawBall(Canvas c, RectF r, Paint p) {
        c.drawCircle(r.centerX(), r.centerY(), Math.min(r.width(), r.height()) * .45f, p);
        c.drawLine(r.left + r.width() * .18f, r.centerY(), r.right - r.width() * .18f, r.centerY(), p);
        c.drawArc(new RectF(r.left + r.width() * .15f, r.top + r.height() * .05f,
                r.centerX() + r.width() * .2f, r.bottom - r.height() * .05f), 285, 150, false, p);
        c.drawArc(new RectF(r.centerX() - r.width() * .2f, r.top + r.height() * .05f,
                r.right - r.width() * .15f, r.bottom - r.height() * .05f), 105, 150, false, p);
    }

    private static void drawLeaf(Canvas c, RectF r, Paint p) {
        Path leaf = new Path();
        leaf.moveTo(r.left, r.bottom);
        leaf.quadTo(r.left + r.width() * .12f, r.top, r.right, r.top);
        leaf.quadTo(r.right - r.width() * .05f, r.bottom - r.height() * .08f, r.left, r.bottom);
        c.drawPath(leaf, p);
        c.drawLine(r.left + r.width() * .12f, r.bottom - r.height() * .08f,
                r.right - r.width() * .1f, r.top + r.height() * .1f, p);
    }

    private static void drawHammer(Canvas c, RectF r, Paint p) {
        c.save();
        c.rotate(-38f, r.centerX(), r.centerY());
        c.drawRoundRect(new RectF(r.centerX() - r.width() * .08f, r.top + r.height() * .2f,
                r.centerX() + r.width() * .08f, r.bottom), r.width() * .04f, r.width() * .04f, p);
        c.drawRoundRect(new RectF(r.left, r.top, r.right, r.top + r.height() * .3f),
                r.width() * .06f, r.width() * .06f, p);
        c.restore();
    }

    private static void drawPhone(Canvas c, RectF r, Paint p) {
        c.drawRoundRect(new RectF(r.left + r.width() * .18f, r.top,
                r.right - r.width() * .18f, r.bottom), r.width() * .12f, r.width() * .12f, p);
        c.drawCircle(r.centerX(), r.bottom - r.height() * .1f, r.width() * .035f, p);
    }

    private static void drawBriefcase(Canvas c, RectF r, Paint p) {
        RectF body = new RectF(r.left, r.top + r.height() * .28f, r.right, r.bottom);
        c.drawRoundRect(body, r.width() * .08f, r.width() * .08f, p);
        c.drawRoundRect(new RectF(r.centerX() - r.width() * .2f, r.top,
                r.centerX() + r.width() * .2f, r.top + r.height() * .32f),
                r.width() * .05f, r.width() * .05f, p);
        c.drawLine(r.left, body.centerY(), r.right, body.centerY(), p);
    }

    private static void drawChat(Canvas c, RectF r, Paint p) {
        RectF a = new RectF(r.left, r.top, r.right - r.width() * .18f, r.bottom - r.height() * .2f);
        RectF b = new RectF(r.left + r.width() * .22f, r.top + r.height() * .26f, r.right, r.bottom);
        c.drawRoundRect(a, r.width() * .12f, r.width() * .12f, p);
        c.drawRoundRect(b, r.width() * .12f, r.width() * .12f, p);
    }

    private static void drawUtilities(Canvas c, RectF r, Paint p) {
        Path drop = new Path();
        drop.moveTo(r.centerX(), r.top);
        drop.quadTo(r.right, r.centerY(), r.centerX(), r.bottom);
        drop.quadTo(r.left, r.centerY(), r.centerX(), r.top);
        c.drawPath(drop, p);
        c.drawLine(r.centerX(), r.top + r.height() * .22f,
                r.centerX() - r.width() * .08f, r.centerY(), p);
    }

    private static void drawShirt(Canvas c, RectF r, Paint p) {
        Path shirt = new Path();
        shirt.moveTo(r.left + r.width() * .28f, r.top);
        shirt.lineTo(r.left, r.top + r.height() * .24f);
        shirt.lineTo(r.left + r.width() * .16f, r.centerY());
        shirt.lineTo(r.left + r.width() * .3f, r.centerY() - r.height() * .08f);
        shirt.lineTo(r.left + r.width() * .3f, r.bottom);
        shirt.lineTo(r.right - r.width() * .3f, r.bottom);
        shirt.lineTo(r.right - r.width() * .3f, r.centerY() - r.height() * .08f);
        shirt.lineTo(r.right - r.width() * .16f, r.centerY());
        shirt.lineTo(r.right, r.top + r.height() * .24f);
        shirt.lineTo(r.right - r.width() * .28f, r.top);
        shirt.quadTo(r.centerX(), r.top + r.height() * .3f, r.left + r.width() * .28f, r.top);
        c.drawPath(shirt, p);
    }

    private static void drawTransfer(Canvas c, RectF r, Paint p) {
        c.drawLine(r.left, r.top + r.height() * .3f, r.right - r.width() * .14f, r.top + r.height() * .3f, p);
        c.drawLine(r.right - r.width() * .14f, r.top + r.height() * .3f,
                r.right - r.width() * .3f, r.top + r.height() * .14f, p);
        c.drawLine(r.right, r.bottom - r.height() * .3f, r.left + r.width() * .14f, r.bottom - r.height() * .3f, p);
        c.drawLine(r.left + r.width() * .14f, r.bottom - r.height() * .3f,
                r.left + r.width() * .3f, r.bottom - r.height() * .14f, p);
    }

    private static void drawCoin(Canvas c, RectF r, Paint p, String mark) {
        c.drawCircle(r.centerX(), r.centerY(), Math.min(r.width(), r.height()) * .46f, p);
        drawMark(c, inset(r, r.width() * .13f), p, mark);
    }

    private static void drawMark(Canvas c, RectF r, Paint line, String mark) {
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(line.getColor());
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(Math.min(r.width(), r.height()) * .64f);
        Paint.FontMetrics fm = text.getFontMetrics();
        float y = r.centerY() - (fm.ascent + fm.descent) / 2f;
        c.drawText(mark == null || mark.isEmpty() ? "记" : mark, r.centerX(), y, text);
    }

    private static final class CategoryDrawable extends Drawable {
        private final CrayonIconSpec spec;
        CategoryDrawable(String type, String category) {
            spec = CrayonIconSpec.forCategory(type, category);
        }
        @Override public void draw(Canvas canvas) {
            Rect b = getBounds();
            drawIcon(canvas, new RectF(b), spec);
        }
        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) { }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        @Override public int getIntrinsicWidth() { return 96; }
        @Override public int getIntrinsicHeight() { return 96; }
    }
}
