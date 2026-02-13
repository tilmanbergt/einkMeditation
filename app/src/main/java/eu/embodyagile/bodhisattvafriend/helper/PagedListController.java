package eu.embodyagile.bodhisattvafriend.helper;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import eu.embodyagile.bodhisattvafriend.R;

public class PagedListController {
    private boolean reflowPosted = false;
    private View.OnTouchListener swipeListener;

    private final ViewGroup pageHost;
    private final View pagerControls;
    private final TextView prev;
    private final TextView status;
    private final TextView next;

    private final List<View> rows = new ArrayList<>();
    private final List<Integer> pageStarts = new ArrayList<>();

    private int currentPage = 0;

    public PagedListController(ViewGroup pageHost,
                               View pagerControls,
                               TextView prev,
                               TextView status,
                               TextView next) {
        this.pageHost = pageHost;
        this.pagerControls = pagerControls;
        this.prev = prev;
        this.status = status;
        this.next = next;

        if (prev != null) prev.setOnClickListener(v -> goPrev());
        if (next != null) next.setOnClickListener(v -> goNext());
    }

    public int getCurrentPage() { return currentPage; }

    public void setRows(List<View> newRows) {
        rows.clear();
        if (newRows != null) rows.addAll(newRows);
        currentPage = 0;
    }

    public void attachSwipe(View swipeSurface) {
        if (swipeSurface == null) return;

        swipeSurface.setClickable(true);

        final boolean[] paged = { false };

        GestureDetector detector = new GestureDetector(swipeSurface.getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    private static final int SWIPE_MIN_DISTANCE = 120;
                    private static final int SWIPE_MIN_VELOCITY = 120;

                    @Override public boolean onDown(MotionEvent e) { return true; }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        if (e1 == null || e2 == null) return false;

                        float dx = e2.getX() - e1.getX();
                        float dy = e2.getY() - e1.getY();

                        if (Math.abs(dy) <= Math.abs(dx)) return false;

                        if (Math.abs(dy) > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_MIN_VELOCITY) {
                            paged[0] = true;
                            if (dy < 0) goNext(); else goPrev();
                            return true;
                        }
                        return false;
                    }
                });

        swipeListener = (v, event) -> {
            // reset per gesture
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) paged[0] = false;

            detector.onTouchEvent(event);

            // only consume if we actually paged
            return paged[0];
        };

        swipeSurface.setOnTouchListener(swipeListener);
    }



    public void computePagesAfterLayoutAndShow() {
        if (pageHost == null) return;

        pageHost.post(new Runnable() {
            @Override public void run() {
                int h = pageHost.getHeight();
                int w = pageHost.getWidth();

                if (h <= 0 || w <= 0) {
                    // not laid out yet, try again next frame
                    pageHost.post(this);
                    return;
                }

                computePages();
                showPage(currentPage);
            }
        });
    }



    public void computePages() {
        pageStarts.clear();
        pageStarts.add(0);

        int available = pageHost.getHeight();
        int width = pageHost.getWidth();

        if (available <= 0 || width <= 0 || rows.isEmpty()) return;

        int sum = 0;
        for (int i = 0; i < rows.size(); i++) {
            View r = rows.get(i);

            int wSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
            int hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            r.measure(wSpec, hSpec);
            int h = r.getMeasuredHeight();

            if (sum + h > available && sum > 0) {
                pageStarts.add(i);
                sum = h;
            } else {
                sum += h;
            }
        }

        int pageCount = getPageCount();
        if (currentPage >= pageCount) currentPage = pageCount - 1;
        if (currentPage < 0) currentPage = 0;
    }

    public int getPageCount() {
        return Math.max(1, pageStarts.size());
    }


    public void installSwipeOn(View v) {
        if (swipeListener != null) {
            v.setOnTouchListener(swipeListener);
        }
    }

    public void showPage(int page) {
        int pageCount = getPageCount();
        currentPage = Math.max(0, Math.min(page, pageCount - 1));

        pageHost.removeAllViews();

        int start = pageStarts.get(currentPage);
        int end = (currentPage == pageCount - 1) ? rows.size() : pageStarts.get(currentPage + 1);

        for (int i = start; i < end; i++) {
            View v = rows.get(i);
            ViewGroup parent = (v.getParent() instanceof ViewGroup) ? (ViewGroup) v.getParent() : null;
            if (parent != null) parent.removeView(v);
            pageHost.addView(v);
            if (swipeListener != null) {
                // attach to the row root
                v.setOnTouchListener(swipeListener);

            }

        }

        boolean visibilityChanged = updateControls();

        // If pager just appeared/disappeared, host height changes (weight=1), so re-page once.
        if (visibilityChanged && !reflowPosted) {
            reflowPosted = true;
            pageHost.post(() -> {
                reflowPosted = false;
                computePages();
                showPage(currentPage);
            });
        }
    }

    private boolean updateControls() {
        int pageCount = getPageCount();

        // Reserve space always: INVISIBLE keeps height, GONE changes layout height.
        if (pagerControls != null) {
            pagerControls.setVisibility(pageCount > 1 ? View.VISIBLE : View.INVISIBLE);
        }

        if (status != null) status.setText((currentPage + 1) + "/" + pageCount);
        if (prev != null) prev.setEnabled(currentPage > 0);
        if (next != null) next.setEnabled(currentPage < pageCount - 1);

        return false; // no longer needed
    }



    private void goPrev() {
        if (currentPage > 0) showPage(currentPage - 1);
    }

    private void goNext() {
        if (currentPage < getPageCount() - 1) showPage(currentPage + 1);
    }
}
